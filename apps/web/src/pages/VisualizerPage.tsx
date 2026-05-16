import { useEffect, useMemo, useState } from 'react'
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
                    <span className="text-[11px] uppercase tracking-[0.32em] text-white/40">
                        TIDAL · Visual EQ {analyser.mode === 'fallback' ? '(procedural)' : ''}
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
