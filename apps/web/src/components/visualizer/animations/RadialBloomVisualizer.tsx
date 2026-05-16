import { useEffect, useRef } from 'react'
import type { VisualizerAnimationProps } from './types'

const RING_SEGMENTS = 96

const hexToRgb = (hex: string): [number, number, number] => {
    const normalized = hex.replace('#', '')
    const value = normalized.length === 3
        ? normalized.split('').map((ch) => ch + ch).join('')
        : normalized
    const num = parseInt(value, 16)
    return [(num >> 16) & 255, (num >> 8) & 255, num & 255]
}

const RadialBloomVisualizer = ({ analyser, accentHex, isPlaying }: VisualizerAnimationProps) => {
    const canvasRef = useRef<HTMLCanvasElement | null>(null)
    const dataRef = useRef<Uint8Array>(new Uint8Array(analyser.binCount))

    useEffect(() => {
        if (dataRef.current.length !== analyser.binCount) {
            dataRef.current = new Uint8Array(analyser.binCount)
        }
    }, [analyser.binCount])

    useEffect(() => {
        const canvas = canvasRef.current
        if (!canvas) {
            return
        }
        const ctx = canvas.getContext('2d')
        if (!ctx) {
            return
        }

        const [accentR, accentG, accentB] = hexToRgb(accentHex)

        const resize = () => {
            const dpr = window.devicePixelRatio || 1
            const rect = canvas.getBoundingClientRect()
            canvas.width = Math.max(1, Math.floor(rect.width * dpr))
            canvas.height = Math.max(1, Math.floor(rect.height * dpr))
            ctx.scale(dpr, dpr)
        }
        resize()
        window.addEventListener('resize', resize)

        let frameId = 0
        let stopped = false

        const tick = () => {
            if (stopped) {
                return
            }
            const buffer = dataRef.current
            analyser.read(buffer)

            const rect = canvas.getBoundingClientRect()
            const width = rect.width
            const height = rect.height
            const cx = width / 2
            const cy = height / 2
            const baseRadius = Math.min(width, height) * 0.18

            ctx.clearRect(0, 0, width, height)

            let bassSum = 0
            const bassBins = Math.max(1, Math.floor(buffer.length * 0.12))
            for (let i = 0; i < bassBins; i += 1) {
                bassSum += buffer[i]
            }
            const bassAvg = bassSum / bassBins / 255

            const segmentCount = RING_SEGMENTS
            const bucketSize = Math.max(1, Math.floor(buffer.length / segmentCount))

            for (let i = 0; i < segmentCount; i += 1) {
                let sum = 0
                const start = i * bucketSize
                const end = Math.min(buffer.length, start + bucketSize)
                for (let j = start; j < end; j += 1) {
                    sum += buffer[j]
                }
                const value = sum / Math.max(1, end - start) / 255
                const angle = (i / segmentCount) * Math.PI * 2
                const inner = baseRadius + bassAvg * 30
                const outer = inner + value * Math.min(width, height) * 0.32

                ctx.beginPath()
                ctx.moveTo(cx + Math.cos(angle) * inner, cy + Math.sin(angle) * inner)
                ctx.lineTo(cx + Math.cos(angle) * outer, cy + Math.sin(angle) * outer)
                ctx.strokeStyle = `rgba(${accentR},${accentG},${accentB},${0.35 + value * 0.65})`
                ctx.lineWidth = 2
                ctx.stroke()
            }

            const glow = ctx.createRadialGradient(cx, cy, baseRadius * 0.4, cx, cy, baseRadius + bassAvg * 80)
            glow.addColorStop(0, `rgba(${accentR},${accentG},${accentB},${0.18 + bassAvg * 0.35})`)
            glow.addColorStop(1, 'rgba(0,0,0,0)')
            ctx.fillStyle = glow
            ctx.beginPath()
            ctx.arc(cx, cy, baseRadius + bassAvg * 80, 0, Math.PI * 2)
            ctx.fill()

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
            window.removeEventListener('resize', resize)
            document.removeEventListener('visibilitychange', handleVisibility)
        }
    }, [analyser, accentHex])

    return (
        <canvas
            ref={canvasRef}
            className="h-full w-full"
            data-state={isPlaying ? 'playing' : 'paused'}
        />
    )
}

export default RadialBloomVisualizer
