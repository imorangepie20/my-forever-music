import { useEffect, useMemo, useRef, useState } from 'react'

export type AnalyserMode = 'analyser' | 'fallback' | 'idle'

export interface TidalAudioAnalyserHandle {
    mode: AnalyserMode
    binCount: number
    read: (target: Uint8Array) => void
}

const FFT_SIZE = 256
const FALLBACK_BIN_COUNT = FFT_SIZE / 2
const VALIDATE_AFTER_MS = 600

const supportsCaptureStream = (element: HTMLMediaElement): element is HTMLMediaElement & { captureStream: () => MediaStream } =>
    typeof (element as HTMLMediaElement & { captureStream?: () => MediaStream }).captureStream === 'function'

const supportsAudioContext = () =>
    typeof window !== 'undefined'
        && (typeof window.AudioContext !== 'undefined'
            || typeof (window as Window & { webkitAudioContext?: typeof AudioContext }).webkitAudioContext !== 'undefined')

const createAudioContext = (): AudioContext | null => {
    if (typeof window === 'undefined') {
        return null
    }
    const ctor = window.AudioContext ?? (window as Window & { webkitAudioContext?: typeof AudioContext }).webkitAudioContext
    return ctor ? new ctor() : null
}

const fillProcedural = (target: Uint8Array, isActive: boolean) => {
    const now = performance.now() / 1000
    const count = target.length
    for (let i = 0; i < count; i += 1) {
        const x = i / count
        if (!isActive) {
            target[i] = Math.round(40 + 12 * Math.sin(now * 1.4 + x * Math.PI * 2.4))
            continue
        }
        const env = 0.55 + 0.45 * Math.exp(-Math.pow((x - 0.22) * 2.4, 2))
        const f =
            0.35 * Math.sin(now * 7 + i * 0.9) +
            0.22 * Math.sin(now * 11 + i * 1.7) +
            0.18 * Math.sin(now * 17 + i * 0.4) +
            0.10 * Math.sin(now * 3 + i * 0.2)
        const normalized = Math.max(0.06, env * (0.55 + 0.45 * f))
        target[i] = Math.round(Math.min(1, normalized) * 220)
    }
}

export function useTidalAudioAnalyser(
    audioElement: HTMLAudioElement | null,
    isPlaying: boolean,
): TidalAudioAnalyserHandle {
    const [mode, setMode] = useState<AnalyserMode>(() => (audioElement ? 'idle' : 'idle'))
    const analyserRef = useRef<AnalyserNode | null>(null)
    const contextRef = useRef<AudioContext | null>(null)
    const sourceRef = useRef<MediaStreamAudioSourceNode | null>(null)
    const mediaStreamRef = useRef<MediaStream | null>(null)
    const isPlayingRef = useRef(isPlaying)

    useEffect(() => {
        isPlayingRef.current = isPlaying
    }, [isPlaying])

    useEffect(() => {
        if (!audioElement || !supportsAudioContext() || !supportsCaptureStream(audioElement)) {
            setMode(audioElement ? 'fallback' : 'idle')
            return
        }

        let cancelled = false
        let validateTimerId: number | null = null

        const ctx = createAudioContext()
        if (!ctx) {
            setMode('fallback')
            return
        }
        contextRef.current = ctx

        try {
            const stream = audioElement.captureStream()
            const source = ctx.createMediaStreamSource(stream)
            const analyser = ctx.createAnalyser()
            analyser.fftSize = FFT_SIZE
            analyser.smoothingTimeConstant = 0.7
            source.connect(analyser)

            mediaStreamRef.current = stream
            sourceRef.current = source
            analyserRef.current = analyser

            if (ctx.state === 'suspended') {
                void ctx.resume().catch(() => undefined)
            }

            setMode('analyser')

            const validation = new Uint8Array(analyser.frequencyBinCount)
            validateTimerId = window.setTimeout(() => {
                if (cancelled || !analyserRef.current) {
                    return
                }
                analyserRef.current.getByteFrequencyData(validation)
                const sum = validation.reduce((acc, value) => acc + value, 0)
                if (sum === 0 && isPlayingRef.current) {
                    setMode('fallback')
                }
            }, VALIDATE_AFTER_MS)
        } catch (error) {
            console.warn('[visualizer] captureStream unavailable, falling back to procedural', error)
            setMode('fallback')
        }

        return () => {
            cancelled = true
            if (validateTimerId !== null) {
                window.clearTimeout(validateTimerId)
            }
            try {
                sourceRef.current?.disconnect()
            } catch {
                /* noop */
            }
            sourceRef.current = null
            analyserRef.current = null
            mediaStreamRef.current = null
            if (contextRef.current && contextRef.current.state !== 'closed') {
                void contextRef.current.close().catch(() => undefined)
            }
            contextRef.current = null
        }
    }, [audioElement])

    useEffect(() => {
        if (mode === 'analyser' && contextRef.current?.state === 'suspended' && isPlaying) {
            void contextRef.current.resume().catch(() => undefined)
        }
    }, [mode, isPlaying])

    return useMemo<TidalAudioAnalyserHandle>(() => ({
        mode,
        binCount: mode === 'analyser' && analyserRef.current
            ? analyserRef.current.frequencyBinCount
            : FALLBACK_BIN_COUNT,
        read: (target: Uint8Array) => {
            if (mode === 'analyser' && analyserRef.current) {
                analyserRef.current.getByteFrequencyData(target)
                return
            }
            fillProcedural(target, isPlayingRef.current)
        },
    }), [mode])
}
