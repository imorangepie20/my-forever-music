import { useEffect, useMemo, useRef, useState } from 'react'
import { AudioRingBuffer } from '@/lib/audioRingBuffer'
import { decodeCompleteAudioData } from '@/lib/segmentDecoder'
import { pcmToByteFrequencyData } from '@/lib/simpleFft'
import type { AnalyserMode, TidalAudioAnalyserHandle } from '@/hooks/useTidalAudioAnalyser'

const FFT_SIZE = 256
const BIN_COUNT = FFT_SIZE / 2
const RING_KEEP_SECONDS = 35

const supportsAudioContext = () =>
    typeof window !== 'undefined'
        && (typeof window.AudioContext !== 'undefined'
            || typeof (window as Window & { webkitAudioContext?: typeof AudioContext }).webkitAudioContext !== 'undefined')

const zero = (target: Uint8Array) => {
    target.fill(0)
}

const errorMessage = (error: unknown, fallback: string) =>
    error instanceof Error && error.message ? error.message : fallback

/**
 * Fetches a single short audio URL (e.g. a Spotify 30s preview), decodes it
 * via Web Audio, and exposes a `TidalAudioAnalyserHandle`-compatible read API
 * so the existing visualizer components can render the preview in real time.
 *
 * The audio element drives playback timing (`audio.currentTime`); the fetched
 * + decoded PCM lives entirely in a ring buffer so the visualizer is not
 * blocked by media-element CORS taint.
 */
export function usePreviewAudioAnalyser(
    previewUrl: string | null | undefined,
    audioElement: HTMLAudioElement | null,
): TidalAudioAnalyserHandle {
    const ringRef = useRef(new AudioRingBuffer(RING_KEEP_SECONDS))
    const audioElementRef = useRef(audioElement)
    const [mode, setMode] = useState<AnalyserMode>('idle')
    const [reason, setReason] = useState<string | null>('waiting for preview url')

    useEffect(() => {
        audioElementRef.current = audioElement
    }, [audioElement])

    useEffect(() => {
        if (!supportsAudioContext()) {
            setMode('unsupported')
            setReason('AudioContext unavailable')
            return
        }

        if (!previewUrl) {
            ringRef.current.clear()
            setMode('idle')
            setReason('waiting for preview url')
            return
        }

        let cancelled = false
        const controller = new AbortController()

        setMode('waiting')
        setReason('fetching preview audio')
        ringRef.current.clear()

        void (async () => {
            try {
                const response = await fetch(previewUrl, { method: 'GET', mode: 'cors', signal: controller.signal })
                if (cancelled) {
                    return
                }
                if (!response.ok) {
                    throw new Error(`preview fetch failed: HTTP ${response.status}`)
                }
                const audioData = await response.arrayBuffer()
                if (cancelled) {
                    return
                }
                const decoded = await decodeCompleteAudioData(audioData)
                if (cancelled) {
                    return
                }
                ringRef.current.append({
                    startTime: 0,
                    sampleRate: decoded.sampleRate,
                    samples: decoded.samples,
                })
                setMode('pcm')
                setReason(`decoded ${decoded.duration.toFixed(2)}s preview`)
            } catch (error) {
                if (cancelled || (error instanceof DOMException && error.name === 'AbortError')) {
                    return
                }
                setMode('error')
                setReason(errorMessage(error, 'preview decode failed'))
            }
        })()

        return () => {
            cancelled = true
            controller.abort()
        }
    }, [previewUrl])

    return useMemo<TidalAudioAnalyserHandle>(() => ({
        mode,
        reason,
        binCount: BIN_COUNT,
        read: (target: Uint8Array) => {
            if (mode !== 'pcm') {
                zero(target)
                return
            }
            const audio = audioElementRef.current
            if (!audio || audio.paused) {
                zero(target)
                return
            }
            const samples = ringRef.current.readWindow(audio.currentTime, FFT_SIZE)
            if (!samples) {
                zero(target)
                return
            }
            pcmToByteFrequencyData(samples, target)
        },
    }), [mode, reason])
}
