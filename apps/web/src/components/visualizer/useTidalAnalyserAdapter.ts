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

function attachAnalyser(audio: HTMLAudioElement): AnalyserAttachment {
    const existing = attachedElements.get(audio)
    if (existing) {
        return existing
    }
    const context = ensureAudioContext()
    // CRITICAL: createMediaElementSource permanently reroutes the audio element's
    // output into the graph. Once called we own the routing forever. If we ever
    // let the graph silence the audio (suspended context, terminal-node analyser,
    // etc.) the user hears nothing. So the topology is parallel — source feeds
    // destination directly AND taps into the analyser. Analyser is a side-branch
    // observer and cannot stop audio reaching the speakers.
    if (context.state !== 'running') {
        throw new Error('AUDIO_CONTEXT_NOT_RUNNING')
    }
    const source = context.createMediaElementSource(audio)
    const analyser = context.createAnalyser()
    analyser.fftSize = ANALYSER_FFT_SIZE
    source.connect(context.destination)
    source.connect(analyser)
    const attachment: AnalyserAttachment = {
        analyser,
        buffer: new Uint8Array(analyser.frequencyBinCount),
        audio,
    }
    attachedElements.set(audio, attachment)
    return attachment
}

/**
 * TIDAL `<audio>` element 에 Web Audio AnalyserNode 를 붙여 실시간 FFT 데이터를
 * `heightsAt(sample): number[]` 형태로 Visualizer 에 공급한다.
 *
 * 토폴로지는 병렬 (source → destination + source → analyser). 따라서 analyser 가
 * 어떤 상태로 가더라도 음향 출력은 영향을 받지 않는다. AudioContext 가 도중에
 * suspended 로 빠지면 audio 자체가 silent 가 되는데, 이 경우 statechange 리스너가
 * 즉시 resume 을 시도하고 실패하면 user gesture 로 재시도한다.
 *
 * Zero-frame 감지는 audio.paused === false + context.state === 'running' 가드가
 * 통과한 상태에서만 카운트되므로, 버퍼링/일시정지 같은 정상 상황에서 false-positive
 * 가 발생하지 않는다.
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
        let stateHandler: (() => void) | null = null
        let observedContext: AudioContext | null = null

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

        const resumeOnGesture = (context: AudioContext) => {
            ensureGestureHandler(() => {
                context.resume().then(() => {
                    if (cancelled) {
                        return
                    }
                    // 이미 attach 되어 있으면 audio 가 graph 를 통해 다시 흐르므로 별도 작업 필요 없음
                    if (!attachmentRef.current) {
                        void tryAttach()
                    } else {
                        setError(null)
                    }
                }).catch(() => {
                    // ignore — wait for the next gesture
                })
            })
        }

        const observeContextState = (context: AudioContext) => {
            if (observedContext === context) {
                return
            }
            observedContext = context
            stateHandler = () => {
                if (cancelled) {
                    return
                }
                if (context.state === 'suspended' && attachmentRef.current) {
                    setError('AudioContext 가 suspended 상태로 빠져 음향이 일시 정지됐습니다. 화면을 한 번 클릭하면 재개됩니다.')
                    resumeOnGesture(context)
                } else if (context.state === 'running') {
                    setError((prev) => (prev && prev.startsWith('AudioContext') ? null : prev))
                }
            }
            context.addEventListener('statechange', stateHandler)
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
                observeContextState(ensureAudioContext())
            } catch (ex) {
                setError(ex instanceof Error ? ex.message : 'AnalyserNode 부착에 실패했습니다.')
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
                doAttach(audio)
                return
            }
            const context = ensureAudioContext()
            observeContextState(context)
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
            resumeOnGesture(context)
        }
        void tryAttach()
        return () => {
            cancelled = true
            if (timer != null) {
                window.clearTimeout(timer)
            }
            removeGestureHandler()
            if (observedContext && stateHandler) {
                observedContext.removeEventListener('statechange', stateHandler)
            }
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
                        // setError can run during render — defer to microtask so we don't break the raf loop
                        queueMicrotask(() => setError(
                            'TIDAL 스트림이 AnalyserNode 에 0 데이터만 전달합니다 (CORS tainted 가능). 바는 procedural envelope 로 fallback 합니다. 음향은 영향 없음.',
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

    // when error fires, the page may want to fall back to procedural — null out heightsAt
    const exposedHeightsAt = error ? null : heightsAt

    return {
        heightsAt: exposedHeightsAt,
        ready: exposedHeightsAt !== null,
        error,
    }
}
