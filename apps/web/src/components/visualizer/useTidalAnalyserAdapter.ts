import { useEffect, useMemo, useRef, useState } from 'react'
import type { VisualizerSample } from '@/components/visualizer/Visualizer'
import { getTidalAudioElement } from '@/lib/tidalStreamPlayback'

interface TidalAnalyserAdapterInput {
    enabled: boolean
    isPlaying: boolean
}

interface TidalAnalyserAdapterResult {
    heightsAt: ((sample: VisualizerSample) => number[]) | null
    ready: boolean
    error: string | null
}

interface AnalyserAttachment {
    analyser: AnalyserNode
    buffer: Uint8Array
    audio: HTMLAudioElement
}

const ANALYSER_FFT_SIZE = 256
const POLL_INTERVAL_MS = 200
const ZERO_FRAME_LIMIT = 60

let sharedAudioContext: AudioContext | null = null
const attachedElements = new WeakMap<HTMLAudioElement, AnalyserAttachment>()

function ensureAudioContext(): AudioContext {
    if (sharedAudioContext) {
        return sharedAudioContext
    }
    const Ctor: typeof AudioContext =
        window.AudioContext ?? (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext
    sharedAudioContext = new Ctor()
    return sharedAudioContext
}

interface CapturableMediaElement extends HTMLAudioElement {
    captureStream?: () => MediaStream
    mozCaptureStream?: () => MediaStream
}

function captureStreamFor(audio: HTMLAudioElement): MediaStream | null {
    const candidate = audio as CapturableMediaElement
    if (typeof candidate.captureStream === 'function') {
        try {
            return candidate.captureStream()
        } catch {
            return null
        }
    }
    if (typeof candidate.mozCaptureStream === 'function') {
        try {
            return candidate.mozCaptureStream()
        } catch {
            return null
        }
    }
    return null
}

function attachAnalyser(audio: HTMLAudioElement): AnalyserAttachment {
    const existing = attachedElements.get(audio)
    if (existing) {
        return existing
    }
    const context = ensureAudioContext()
    if (context.state !== 'running') {
        throw new Error('AUDIO_CONTEXT_NOT_RUNNING')
    }
    // IMPORTANT: We deliberately use captureStream() instead of
    // createMediaElementSource(). createMediaElementSource permanently reroutes
    // the audio element's output into the Web Audio graph — if anything goes
    // wrong in the graph (context suspended, page navigated away mid-playback,
    // crossed-up state across providers) the user loses audio entirely. With
    // captureStream we open a parallel observation stream that has no effect on
    // playback routing, so the element keeps playing through its native pipeline
    // even if the analyser fails or this hook unmounts.
    const stream = captureStreamFor(audio)
    if (!stream) {
        throw new Error('CAPTURESTREAM_UNSUPPORTED')
    }
    const audioTracks = stream.getAudioTracks()
    if (audioTracks.length === 0) {
        throw new Error('CAPTURESTREAM_NO_AUDIO_TRACK')
    }
    const source = context.createMediaStreamSource(stream)
    const analyser = context.createAnalyser()
    analyser.fftSize = ANALYSER_FFT_SIZE
    source.connect(analyser)
    // analyser is a side-branch observer — no connection to destination.
    const attachment: AnalyserAttachment = {
        analyser,
        buffer: new Uint8Array(analyser.frequencyBinCount),
        audio,
    }
    attachedElements.set(audio, attachment)
    return attachment
}

/**
 * TIDAL `<audio>` element 의 출력을 `audio.captureStream()` 으로 따와 `AnalyserNode` 로
 * 흘려 보내고, 그 FFT 데이터를 Visualizer 의 `heightsAt` 에 공급한다.
 *
 * `createMediaElementSource` 를 쓰지 않는 이유: 한 번 호출되면 audio 출력이 영구히
 * Web Audio graph 로 redirect 되어, 이후 (다른 페이지에서 같은 audio element 로 재생되든,
 * AudioContext 가 suspended 로 빠지든) 음향이 silent 가 될 위험이 있다. captureStream 은
 * 평행한 observation stream 이라 재생 path 를 절대 가로채지 않으므로, visualizer 가
 * 실패하더라도 음향에 영향이 없다.
 *
 * captureStream 이 지원되지 않거나 (예: 일부 구버전 브라우저) audio track 이 비어 있으면
 * error 를 설정하고 procedural envelope 로 fallback 한다.
 */
export function useTidalAnalyserAdapter(input: TidalAnalyserAdapterInput): TidalAnalyserAdapterResult {
    const { enabled, isPlaying } = input
    const [error, setError] = useState<string | null>(null)
    const [ready, setReady] = useState(false)
    const attachmentRef = useRef<AnalyserAttachment | null>(null)
    const zeroFramesRef = useRef(0)
    const isPlayingRef = useRef(isPlaying)

    useEffect(() => {
        isPlayingRef.current = isPlaying
    }, [isPlaying])

    useEffect(() => {
        if (!enabled) {
            attachmentRef.current = null
            setReady(false)
            setError(null)
            zeroFramesRef.current = 0
            return
        }
        let cancelled = false
        let timer: number | null = null
        let gestureHandler: (() => void) | null = null

        const removeGestureHandler = () => {
            if (gestureHandler) {
                document.removeEventListener('pointerdown', gestureHandler, true)
                document.removeEventListener('keydown', gestureHandler, true)
                gestureHandler = null
            }
        }

        const ensureGestureHandler = (onTrigger: () => void) => {
            if (gestureHandler) {
                return
            }
            gestureHandler = () => {
                removeGestureHandler()
                onTrigger()
            }
            document.addEventListener('pointerdown', gestureHandler, true)
            document.addEventListener('keydown', gestureHandler, true)
        }

        const doAttach = (audio: HTMLAudioElement) => {
            if (cancelled) {
                return
            }
            try {
                const attachment = attachAnalyser(audio)
                attachmentRef.current = attachment
                zeroFramesRef.current = 0
                setReady(true)
                setError(null)
                removeGestureHandler()
            } catch (ex) {
                const message = ex instanceof Error ? ex.message : 'AnalyserNode 부착에 실패했습니다.'
                if (message === 'CAPTURESTREAM_UNSUPPORTED') {
                    setError('이 브라우저는 audio.captureStream() 을 지원하지 않습니다. 바는 procedural envelope 로 fallback 합니다. 음향은 영향 없음.')
                } else if (message === 'CAPTURESTREAM_NO_AUDIO_TRACK') {
                    setError('TIDAL 스트림이 아직 audio track 을 노출하지 않았습니다 (재생 시작 전이거나 일시정지). 재생을 다시 시작해 보세요.')
                } else if (message === 'AUDIO_CONTEXT_NOT_RUNNING') {
                    // gesture handler will retry — no error message needed yet
                } else {
                    setError(message)
                }
                setReady(false)
            }
        }

        const tryAttach = async () => {
            if (cancelled) {
                return
            }
            const audio = getTidalAudioElement()
            if (!audio) {
                timer = window.setTimeout(() => { void tryAttach() }, POLL_INTERVAL_MS)
                return
            }
            const existing = attachedElements.get(audio)
            if (existing) {
                attachmentRef.current = existing
                zeroFramesRef.current = 0
                setReady(true)
                setError(null)
                return
            }
            const context = ensureAudioContext()
            if (context.state !== 'running') {
                try {
                    await context.resume()
                } catch {
                    // resume requires a user gesture in most browsers; fall through.
                }
            }
            if (cancelled) {
                return
            }
            if (context.state === 'running') {
                doAttach(audio)
                return
            }
            ensureGestureHandler(() => {
                context.resume().then(() => {
                    if (!cancelled) {
                        void tryAttach()
                    }
                }).catch(() => {
                    // ignore — wait for the next gesture
                })
            })
        }
        void tryAttach()
        return () => {
            cancelled = true
            if (timer != null) {
                window.clearTimeout(timer)
            }
            removeGestureHandler()
        }
    }, [enabled])

    const heightsAt = useMemo(() => {
        if (!ready) {
            return null
        }
        return (sample: VisualizerSample) => {
            const attachment = attachmentRef.current
            if (!attachment) {
                return new Array(sample.count).fill(0.06)
            }
            const { analyser, buffer, audio } = attachment
            analyser.getByteFrequencyData(buffer)

            const context = sharedAudioContext
            const audioActive = isPlayingRef.current && !audio.paused
            const contextActive = context !== null && context.state === 'running'
            if (audioActive && contextActive) {
                let maxValue = 0
                for (let j = 0; j < buffer.length; j++) {
                    if (buffer[j] > maxValue) {
                        maxValue = buffer[j]
                    }
                }
                if (maxValue === 0) {
                    zeroFramesRef.current += 1
                    if (zeroFramesRef.current === ZERO_FRAME_LIMIT) {
                        queueMicrotask(() => setError(
                            'TIDAL captureStream 에서 0 데이터만 전달됩니다 (CORS tainted 가능). 바는 procedural envelope 로 fallback 합니다. 음향은 영향 없음.',
                        ))
                    }
                } else {
                    zeroFramesRef.current = 0
                }
            } else {
                zeroFramesRef.current = 0
            }

            const out = new Array(sample.count)
            const binCount = buffer.length
            for (let i = 0; i < sample.count; i++) {
                const lo = Math.floor((i / sample.count) * binCount)
                const hi = Math.max(lo + 1, Math.floor(((i + 1) / sample.count) * binCount))
                let peak = 0
                for (let j = lo; j < hi && j < binCount; j++) {
                    if (buffer[j] > peak) {
                        peak = buffer[j]
                    }
                }
                out[i] = peak / 255
            }
            return out
        }
    }, [ready])

    const exposedHeightsAt = error ? null : heightsAt

    return {
        heightsAt: exposedHeightsAt,
        ready: exposedHeightsAt !== null,
        error,
    }
}
