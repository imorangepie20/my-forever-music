import { useEffect, useRef } from 'react'
import type { VisualizerAnimationProps } from './types'

const MAX_PARTICLES = 220
const SPAWN_THRESHOLD = 0.12

interface Particle {
    x: number
    y: number
    vx: number
    vy: number
    life: number
    maxLife: number
    size: number
}

const hexToRgb = (hex: string): [number, number, number] => {
    const normalized = hex.replace('#', '')
    const value = normalized.length === 3
        ? normalized.split('').map((ch) => ch + ch).join('')
        : normalized
    const num = parseInt(value, 16)
    return [(num >> 16) & 255, (num >> 8) & 255, num & 255]
}

const ParticleDriftVisualizer = ({ analyser, accentHex, isPlaying }: VisualizerAnimationProps) => {
    const canvasRef = useRef<HTMLCanvasElement | null>(null)
    const dataRef = useRef<Uint8Array>(new Uint8Array(analyser.binCount))
    const particlesRef = useRef<Particle[]>([])

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
            ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
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

            let bassSum = 0
            const bassBins = Math.max(1, Math.floor(buffer.length * 0.12))
            for (let i = 0; i < bassBins; i += 1) {
                bassSum += buffer[i]
            }
            const bassAvg = bassSum / bassBins / 255

            let trebleSum = 0
            const trebleStart = Math.floor(buffer.length * 0.6)
            for (let i = trebleStart; i < buffer.length; i += 1) {
                trebleSum += buffer[i]
            }
            const trebleAvg = trebleSum / Math.max(1, buffer.length - trebleStart) / 255

            const spawnCount = Math.floor(bassAvg * 12 + trebleAvg * 6)
            if (bassAvg > SPAWN_THRESHOLD) {
                for (let i = 0; i < spawnCount; i += 1) {
                    if (particlesRef.current.length >= MAX_PARTICLES) {
                        break
                    }
                    const angle = Math.random() * Math.PI * 2
                    const speed = 0.6 + bassAvg * 3.2 + Math.random() * 1.4
                    const maxLife = 1200 + Math.random() * 1600
                    particlesRef.current.push({
                        x: width / 2 + (Math.random() - 0.5) * width * 0.08,
                        y: height / 2 + (Math.random() - 0.5) * height * 0.08,
                        vx: Math.cos(angle) * speed,
                        vy: Math.sin(angle) * speed - 0.4,
                        life: 0,
                        maxLife,
                        size: 1.5 + Math.random() * 2.5,
                    })
                }
            }

            ctx.fillStyle = 'rgba(0,0,0,0.18)'
            ctx.fillRect(0, 0, width, height)

            const remaining: Particle[] = []
            for (const particle of particlesRef.current) {
                particle.life += 16
                if (particle.life >= particle.maxLife) {
                    continue
                }
                particle.x += particle.vx
                particle.y += particle.vy
                particle.vy += 0.012
                const alpha = 1 - particle.life / particle.maxLife
                ctx.beginPath()
                ctx.arc(particle.x, particle.y, particle.size, 0, Math.PI * 2)
                ctx.fillStyle = `rgba(${accentR},${accentG},${accentB},${alpha * 0.85})`
                ctx.fill()
                remaining.push(particle)
            }
            particlesRef.current = remaining

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
            particlesRef.current = []
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

export default ParticleDriftVisualizer
