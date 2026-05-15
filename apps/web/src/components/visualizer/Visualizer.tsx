import { useEffect, useMemo, useRef, useState } from 'react'

export type VisualizerMode = 'idle' | 'spotify' | 'tidal'

export interface VisualizerProps {
    /** when false, falls back to a slow ambient ripple regardless of `mode` */
    playing: boolean
    /** picks the procedural envelope when no real `heightsAt` adapter is provided */
    mode: VisualizerMode
    /** number of bars rendered (default 64 matches the reference player) */
    bars?: number
    /**
     * Real-data adapter. Receives the running clock `t` (seconds) plus `bars` and `mode`
     * and returns an array of bar heights normalized roughly to [0, 1]. When omitted,
     * the visualizer falls back to a deterministic procedural envelope so the page
     * still has a visual signal before adapters are wired.
     */
    heightsAt?: (input: VisualizerSample) => number[]
}

export interface VisualizerSample {
    count: number
    t: number
    mode: VisualizerMode
    beatStrength: number
}

/**
 * Bar-style audio visualizer. The component owns the requestAnimationFrame loop and
 * delegates amplitude calculation to `heightsAt`. The default fallback is the same
 * procedural envelope used by the reference Hi-Fi player so the bars react to mode +
 * playing state even without a bound data source.
 */
export default function Visualizer({ playing, mode, bars = 64, heightsAt = defaultHeightsAt }: VisualizerProps) {
    const [heights, setHeights] = useState<number[]>(() => new Array(bars).fill(0.2))
    const tRef = useRef(0)
    const rafRef = useRef(0)

    useEffect(() => {
        setHeights((prev) => (prev.length === bars ? prev : new Array(bars).fill(0.2)))
    }, [bars])

    useEffect(() => {
        let prev = performance.now()
        const loop = (now: number) => {
            const dt = Math.min(50, now - prev) / 1000
            prev = now
            tRef.current += dt
            const t = tRef.current
            const activeMode: VisualizerMode = playing ? mode : 'idle'
            const beatStrength = activeMode === 'spotify' ? computeBeatStrength(t) : 0
            const next = heightsAt({ count: bars, t, mode: activeMode, beatStrength })
            setHeights(next)
            rafRef.current = requestAnimationFrame(loop)
        }
        rafRef.current = requestAnimationFrame(loop)
        return () => cancelAnimationFrame(rafRef.current)
    }, [playing, mode, bars, heightsAt])

    const maxIdx = useMemo(() => {
        let m = 0
        let mv = -Infinity
        heights.forEach((h, i) => {
            if (h > mv) {
                mv = h
                m = i
            }
        })
        return m
    }, [heights])

    return (
        <div
            className="flex h-32 w-full max-w-2xl items-end justify-center gap-[3px] px-1"
            aria-hidden="true"
        >
            {heights.map((h, i) => {
                const hot = i % 7 === 0 || i === maxIdx
                return (
                    <span
                        key={i}
                        className={`block w-[5px] rounded-t-sm transition-[height] duration-75 ease-linear ${
                            hot ? 'bg-hud-accent-primary' : 'bg-hud-text-primary/85'
                        }`}
                        style={{ height: `${Math.max(6, h * 100)}%` }}
                    />
                )
            })}
        </div>
    )
}

const BEAT_BPM = 112
const BEAT_PERIOD = 60 / BEAT_BPM

function computeBeatStrength(t: number) {
    const beatPhase = (t % BEAT_PERIOD) / BEAT_PERIOD
    return Math.pow(1 - beatPhase, 2.2)
}

/**
 * Deterministic procedural envelope. Same shape as the reference player so the page
 * has visual life when no real adapter has been bound yet (e.g. cold-start, between
 * tracks, or while the audio-analysis fetch is in-flight).
 */
function defaultHeightsAt({ count, t, mode, beatStrength }: VisualizerSample): number[] {
    const out = new Array(count)
    for (let i = 0; i < count; i++) {
        const x = i / count
        if (mode === 'idle') {
            out[i] = 0.12 + 0.05 * Math.sin(t * 1.4 + x * Math.PI * 2.4)
        } else if (mode === 'spotify') {
            const bass = Math.exp(-Math.pow((x - 0.18) * 4.4, 2)) * (0.55 + 0.4 * beatStrength)
            const mid = Math.exp(-Math.pow((x - 0.5) * 4.0, 2)) * (0.35 + 0.3 * Math.sin(t * 4 + x * 6))
            const high = Math.exp(-Math.pow((x - 0.85) * 6.0, 2)) * (0.25 + 0.2 * Math.sin(t * 9 + x * 10))
            const wobble = 0.05 * Math.sin(t * 13 + i * 0.7)
            out[i] = Math.max(0.06, bass + mid * 0.8 + high * 0.7 + wobble)
        } else {
            const env = 0.55 + 0.45 * Math.exp(-Math.pow((x - 0.22) * 2.4, 2))
            const f =
                0.35 * Math.sin(t * 7 + i * 0.9) +
                0.22 * Math.sin(t * 11 + i * 1.7) +
                0.18 * Math.sin(t * 17 + i * 0.4) +
                0.1 * Math.sin(t * 3 + i * 0.2)
            out[i] = Math.max(0.06, env * (0.55 + 0.45 * f))
        }
    }
    return out
}
