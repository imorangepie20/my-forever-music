import { useEffect, useMemo, useRef, useState } from 'react'
import { ArrowLeft } from 'lucide-react'
import { Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import ControlsBar from '@/components/visualizer/ControlsBar'
import EqOverlay, { type AnimationId } from '@/components/visualizer/EqOverlay'
import FullscreenCover from '@/components/visualizer/FullscreenCover'
import QueueRail from '@/components/visualizer/QueueRail'
import { usePlayback } from '@/contexts/PlaybackContext'
import { useDominantColor } from '@/hooks/useDominantColor'
import { useTidalAudioAnalyser } from '@/hooks/useTidalAudioAnalyser'
import { resolvePlaybackPlatformId } from '@/lib/musicPlayback'
import { getTidalAudioElement } from '@/lib/tidalStreamPlayback'

const isAnimationId = (value: string | null): value is AnimationId =>
    value === 'bars' || value === 'radial' || value === 'particle'

const VisualizerPage = () => {
    const navigate = useNavigate()
    const [searchParams] = useSearchParams()
    const { currentItem, queue, currentIndex, isPlaying } = usePlayback()
    const [audioElement, setAudioElement] = useState<HTMLAudioElement | null>(() => getTidalAudioElement())

    useEffect(() => {
        if (audioElement) {
            return
        }
        const id = window.setInterval(() => {
            const element = getTidalAudioElement()
            if (element) {
                setAudioElement(element)
                window.clearInterval(id)
            }
        }, 250)
        return () => window.clearInterval(id)
    }, [audioElement])

    const analyser = useTidalAudioAnalyser(audioElement, isPlaying)
    const color = useDominantColor(currentItem?.imageUrl, `${currentItem?.sourcePlatform ?? 'tidal'}-${currentItem?.title ?? 'unknown'}`)

    const forcedAnimation = useMemo(() => {
        const raw = searchParams.get('animation')
        return isAnimationId(raw) ? raw : null
    }, [searchParams])

    const diagnosticsBufferRef = useRef<Uint8Array>(new Uint8Array(analyser.binCount))
    const [diagnostics, setDiagnostics] = useState({ avg: 0, peak: 0 })

    useEffect(() => {
        if (diagnosticsBufferRef.current.length !== analyser.binCount) {
            diagnosticsBufferRef.current = new Uint8Array(analyser.binCount)
        }
        const id = window.setInterval(() => {
            const buf = diagnosticsBufferRef.current
            analyser.read(buf)
            let total = 0
            let peak = 0
            for (let i = 0; i < buf.length; i += 1) {
                total += buf[i]
                if (buf[i] > peak) {
                    peak = buf[i]
                }
            }
            setDiagnostics({ avg: Math.round(total / buf.length), peak })
        }, 500)
        return () => window.clearInterval(id)
    }, [analyser])

    if (!currentItem) {
        return <Navigate to="/" replace />
    }
    if (resolvePlaybackPlatformId(currentItem) !== 'tidal') {
        return <Navigate to="/" replace />
    }

    const trackKey = `${currentItem.sourcePlatform}-${currentItem.id}`

    return (
        <div className="fixed inset-0 flex h-screen w-screen bg-black text-white">
            <QueueRail queue={queue} currentIndex={currentIndex} accentHex={color.hex} />
            <main className="relative flex min-w-0 flex-1 flex-col">
                <header className="relative z-20 flex items-center justify-between px-6 py-4">
                    <button
                        type="button"
                        onClick={() => navigate(-1)}
                        className="flex items-center gap-2 rounded-full border border-white/15 bg-black/40 px-3 py-1.5 text-sm text-white/80 transition hover:text-white"
                        aria-label="Exit visualizer"
                    >
                        <ArrowLeft size={16} />
                        <span>Back</span>
                    </button>
                    <span className="flex items-center gap-3 text-[11px] uppercase tracking-[0.32em] text-white/40">
                        <span>
                            TIDAL · Visual EQ · {analyser.mode}
                        </span>
                        <span className="rounded-full border border-white/15 bg-black/40 px-2 py-0.5 font-mono normal-case tracking-normal text-white/70">
                            avg {diagnostics.avg} · peak {diagnostics.peak}
                            {analyser.reason ? ` · ${analyser.reason}` : ''}
                        </span>
                    </span>
                </header>
                <section className="relative flex flex-1 items-center justify-center">
                    <FullscreenCover
                        imageUrl={currentItem.imageUrl}
                        seed={trackKey}
                        label={currentItem.title}
                        accentHex={color.hex}
                    />
                    <div className="pointer-events-none absolute inset-0 z-10">
                        <EqOverlay
                            analyser={analyser}
                            accentHex={color.hex}
                            isPlaying={isPlaying}
                            trackKey={trackKey}
                            forcedAnimation={forcedAnimation}
                        />
                    </div>
                </section>
                <ControlsBar accentHex={color.hex} />
            </main>
        </div>
    )
}

export default VisualizerPage
