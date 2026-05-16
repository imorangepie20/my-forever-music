import { useEffect, useMemo, useRef, useState } from 'react'
import { AudioRingBuffer } from '@/lib/audioRingBuffer'
import { decodeCompleteAudioData, decodeFragmentedMp4Segment } from '@/lib/segmentDecoder'
import { pcmToByteFrequencyData } from '@/lib/simpleFft'
import { subscribeTidalAudioCapture, type CapturedTidalAudioSegment } from '@/lib/tidalAudioCapture'
import { fetchTidalPlaybackAnalysisAudio } from '@/services/api'

export type AnalyserMode = 'pcm' | 'waiting' | 'idle' | 'error' | 'unsupported'

export interface TidalAudioAnalyserHandle {
    mode: AnalyserMode
    reason: string | null
    binCount: number
    read: (target: Uint8Array) => void
}

const FFT_SIZE = 256
const BIN_COUNT = FFT_SIZE / 2

const supportsAudioContext = () =>
    typeof window !== 'undefined'
        && (typeof window.AudioContext !== 'undefined'
            || typeof (window as Window & { webkitAudioContext?: typeof AudioContext }).webkitAudioContext !== 'undefined')

const zero = (target: Uint8Array) => {
    target.fill(0)
}

const errorMessage = (error: unknown, fallback: string) =>
    error instanceof Error && error.message ? error.message : fallback

const resolveSegmentStart = (segment: CapturedTidalAudioSegment, audioElement: HTMLAudioElement | null) => {
    if (typeof segment.startTime === 'number' && Number.isFinite(segment.startTime)) {
        return segment.startTime
    }
    if (audioElement && Number.isFinite(audioElement.currentTime)) {
        return audioElement.currentTime
    }
    return 0
}

export function useTidalAudioAnalyser(
    audioElement: HTMLAudioElement | null,
    isPlaying: boolean,
): TidalAudioAnalyserHandle {
    const ringRef = useRef(new AudioRingBuffer())
    const decodeQueueRef = useRef<Promise<void>>(Promise.resolve())
    const audioElementRef = useRef(audioElement)
    const isPlayingRef = useRef(isPlaying)
    const [mode, setMode] = useState<AnalyserMode>('idle')
    const [reason, setReason] = useState<string | null>('waiting for TIDAL playback')

    useEffect(() => {
        audioElementRef.current = audioElement
    }, [audioElement])

    useEffect(() => {
        isPlayingRef.current = isPlaying
    }, [isPlaying])

    useEffect(() => {
        if (!supportsAudioContext()) {
            setMode('unsupported')
            setReason('AudioContext unavailable')
            return
        }

        let mounted = true

        const decodeSegment = async (segment: CapturedTidalAudioSegment) => {
            if (segment.kind !== 'media') {
                return
            }
            if (!segment.initSegment) {
                if (mounted) {
                    setMode('waiting')
                    setReason('waiting for HLS init segment')
                }
                return
            }

            try {
                const decoded = await decodeFragmentedMp4Segment(segment.initSegment, segment.payload)
                if (!mounted) {
                    return
                }

                ringRef.current.append({
                    startTime: resolveSegmentStart(segment, audioElementRef.current),
                    sampleRate: decoded.sampleRate,
                    samples: decoded.samples,
                })
                setMode('pcm')
                setReason(`decoded ${decoded.duration.toFixed(2)}s segment`)
            } catch (error) {
                if (!mounted) {
                    return
                }
                const message = error instanceof Error ? error.message : 'segment decode failed'
                setMode('error')
                setReason(message)
            }
        }

        const decodeDirectStream = async (
            url: string,
            startTime: number | null,
            userId: string,
            trackId: string,
            quality: string,
        ) => {
            if (mounted) {
                setMode('waiting')
                setReason('fetching direct TIDAL stream for analysis')
            }

            let audioData: ArrayBuffer
            try {
                const response = await fetch(url, { method: 'GET', mode: 'cors' })
                if (!response.ok) {
                    throw new Error(`direct stream fetch failed: HTTP ${response.status}`)
                }
                audioData = await response.arrayBuffer()
            } catch (directFetchError) {
                const directMessage = errorMessage(directFetchError, 'browser direct fetch failed')
                if (mounted) {
                    setReason(`browser direct fetch failed; trying API analysis fetch (${directMessage})`)
                }

                try {
                    audioData = await fetchTidalPlaybackAnalysisAudio(userId, trackId, quality)
                } catch (apiFetchError) {
                    throw new Error(
                        `browser direct fetch failed (${directMessage}); API analysis fetch failed (${errorMessage(apiFetchError, 'unknown error')})`,
                    )
                }
            }

            try {
                const decoded = await decodeCompleteAudioData(audioData)
                if (!mounted) {
                    return
                }

                ringRef.current.clear()
                ringRef.current.append({
                    startTime: startTime ?? 0,
                    sampleRate: decoded.sampleRate,
                    samples: decoded.samples,
                })
                setMode('pcm')
                setReason(`decoded direct ${decoded.duration.toFixed(2)}s stream`)
            } catch (error) {
                if (!mounted) {
                    return
                }
                setMode('error')
                setReason(errorMessage(error, 'direct stream decode failed'))
            }
        }

        const unsubscribe = subscribeTidalAudioCapture((event) => {
            if (event.type === 'reset') {
                ringRef.current.clear()
                if (mounted) {
                    setMode('waiting')
                    setReason('waiting for HLS audio segment')
                }
                return
            }
            if (event.type === 'source') {
                if (event.source === 'native-hls') {
                    setMode('unsupported')
                    setReason('native HLS playback does not expose HLS.js segments')
                } else if (event.source === 'direct') {
                    setMode('waiting')
                    setReason('direct TIDAL stream detected')
                } else {
                    setMode('waiting')
                    setReason('waiting for HLS audio segment')
                }
                return
            }
            if (event.type === 'direct-stream') {
                decodeQueueRef.current = decodeQueueRef.current
                    .catch(() => undefined)
                    .then(() => decodeDirectStream(event.url, event.startTime, event.userId, event.trackId, event.quality))
                return
            }

            decodeQueueRef.current = decodeQueueRef.current
                .catch(() => undefined)
                .then(() => decodeSegment(event.segment))
        })

        setMode('waiting')
        setReason('waiting for HLS audio segment')

        return () => {
            mounted = false
            unsubscribe()
        }
    }, [])

    return useMemo<TidalAudioAnalyserHandle>(() => ({
        mode,
        reason,
        binCount: BIN_COUNT,
        read: (target: Uint8Array) => {
            if (mode !== 'pcm' || !isPlayingRef.current) {
                zero(target)
                return
            }

            const samples = ringRef.current.readWindow(audioElementRef.current?.currentTime ?? Number.NaN, FFT_SIZE)
            if (!samples) {
                zero(target)
                return
            }
            pcmToByteFrequencyData(samples, target)
        },
    }), [mode, reason])
}
