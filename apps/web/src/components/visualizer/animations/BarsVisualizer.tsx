import { useEffect, useMemo, useRef } from 'react'
import type { VisualizerAnimationProps } from './types'

const BAR_COUNT = 64

const BarsVisualizer = ({ analyser, accentHex, isPlaying }: VisualizerAnimationProps) => {
    const barRefs = useRef<Array<HTMLSpanElement | null>>([])
    const dataRef = useRef<Uint8Array>(new Uint8Array(analyser.binCount))

    const bars = useMemo(() => Array.from({ length: BAR_COUNT }, (_, index) => index), [])

    useEffect(() => {
        if (dataRef.current.length !== analyser.binCount) {
            dataRef.current = new Uint8Array(analyser.binCount)
        }
    }, [analyser.binCount])

    useEffect(() => {
        let frameId = 0
        let stopped = false

        const tick = () => {
            if (stopped) {
                return
            }
            const buffer = dataRef.current
            analyser.read(buffer)

            const bucketSize = Math.max(1, Math.floor(buffer.length / BAR_COUNT))
            let maxValue = -1
            let maxIndex = 0
            const heights: number[] = new Array(BAR_COUNT)

            for (let i = 0; i < BAR_COUNT; i += 1) {
                let sum = 0
                const start = i * bucketSize
                const end = Math.min(buffer.length, start + bucketSize)
                for (let j = start; j < end; j += 1) {
                    sum += buffer[j]
                }
                const avg = sum / Math.max(1, end - start) / 255
                heights[i] = avg
                if (avg > maxValue) {
                    maxValue = avg
                    maxIndex = i
                }
            }

            for (let i = 0; i < BAR_COUNT; i += 1) {
                const element = barRefs.current[i]
                if (!element) {
                    continue
                }
                const heightPct = Math.max(6, heights[i] * 100)
                element.style.height = `${heightPct}%`
                const isHot = i % 7 === 0 || i === maxIndex
                element.style.background = isHot ? accentHex : 'rgba(255,255,255,0.78)'
            }

            frameId = requestAnimationFrame(tick)
        }

        const handleVisibility = () => {
            if (document.visibilityState === 'hidden') {
                cancelAnimationFrame(frameId)
            } else if (!stopped) {
                frameId = requestAnimationFrame(tick)
            }
        }

        frameId = requestAnimationFrame(tick)
        document.addEventListener('visibilitychange', handleVisibility)

        return () => {
            stopped = true
            cancelAnimationFrame(frameId)
            document.removeEventListener('visibilitychange', handleVisibility)
        }
    }, [analyser, accentHex])

    return (
        <div
            className="pointer-events-none flex h-32 w-full max-w-3xl items-end justify-center gap-[3px] px-2"
            data-state={isPlaying ? 'playing' : 'paused'}
        >
            {bars.map((index) => (
                <span
                    key={index}
                    ref={(node) => {
                        barRefs.current[index] = node
                    }}
                    className="block w-[5px] rounded-t-[2px] transition-[background-color] duration-300"
                    style={{ height: '6%', background: 'rgba(255,255,255,0.78)' }}
                />
            ))}
        </div>
    )
}

export default BarsVisualizer
