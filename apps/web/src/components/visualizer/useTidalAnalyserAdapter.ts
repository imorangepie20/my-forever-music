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
}

const ANALYSER_FFT_SIZE = 256
const POLL_INTERVAL_MS = 200
const ZERO_FRAME_LIMIT = 30

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
    const source = context.createMediaElementSource(audio)
    const analyser = context.createAnalyser()
    analyser.fftSize = ANALYSER_FFT_SIZE
    source.connect(analyser)
    // Keep audio audible: AnalyserNode does not pass signal to destination by default
    // when it's the terminal node, so we route through it.
    analyser.connect(context.destination)
    const attachment: AnalyserAttachment = {
        analyser,
        buffer: new Uint8Array(analyser.frequencyBinCount),
    }
    attachedElements.set(audio, attachment)
    return attachment
}

/**
 * TIDAL `<audio>` element 에 Web Audio AnalyserNode 를 붙여 실시간 FFT 데이터를
 * `heightsAt(sample): number[]` 형태로 Visualizer 에 공급한다.
 *
 * CORS 처리는 silent fallback 을 피한다 — stream 이 tainted 라 AnalyserNode 가
 * 0 데이터만 반환하는 경우 (재생은 됨, FFT 만 silent) ZERO_FRAME_LIMIT 프레임 후
 * `error` 를 set 한다. 페이지가 그 사실을 운영자에게 안내한다.
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

        const tryAttach = () => {
            if (cancelled) {
                return
            }
            const audio = getTidalAudioElement()
            if (!audio) {
                timer = window.setTimeout(tryAttach, POLL_INTERVAL_MS)
                return
            }
            try {
                const attachment = attachAnalyser(audio)
                attachmentRef.current = attachment
                zeroFramesRef.current = 0
                setReady(true)
                setError(null)
                if (sharedAudioContext && sharedAudioContext.state === 'suspended') {
                    void sharedAudioContext.resume()
                }
            } catch (ex) {
                setError(ex instanceof Error ? ex.message : 'AnalyserNode 부착에 실패했습니다.')
                setReady(false)
            }
        }
        tryAttach()
        return () => {
            cancelled = true
            if (timer != null) {
                window.clearTimeout(timer)
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
            const { analyser, buffer } = attachment
            analyser.getByteFrequencyData(buffer)

            let maxValue = 0
            for (let j = 0; j < buffer.length; j++) {
                if (buffer[j] > maxValue) {
                    maxValue = buffer[j]
                }
            }
            if (isPlayingRef.current) {
                if (maxValue === 0) {
                    zeroFramesRef.current += 1
                    if (zeroFramesRef.current === ZERO_FRAME_LIMIT) {
                        // setError can run during render — defer to microtask so we don't break the raf loop
                        queueMicrotask(() => setError(
                            'TIDAL 스트림이 AnalyserNode 에 0 데이터만 전달합니다 (CORS tainted 가능). 바는 procedural envelope 로 fallback 합니다.',
                        ))
                    }
                } else {
                    zeroFramesRef.current = 0
                }
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
