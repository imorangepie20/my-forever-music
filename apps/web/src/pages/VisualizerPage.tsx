import { useNavigate } from 'react-router-dom'
import { AlertTriangle, ArrowLeft, Pause, Play, SkipBack, SkipForward } from 'lucide-react'
import Button from '@/components/common/Button'
import Visualizer, { type VisualizerMode } from '@/components/visualizer/Visualizer'
import { useSpotifyAudioAnalysisAdapter } from '@/components/visualizer/useSpotifyAudioAnalysisAdapter'
import { useAuthSession } from '@/contexts/AuthSessionContext'
import { usePlayback } from '@/contexts/PlaybackContext'
import { formatDuration, resolveSpotifyTrackId } from '@/lib/musicPlayback'

const VISUALIZER_BARS = 64

const resolveMode = (sourcePlatform: string | undefined | null): VisualizerMode => {
    const normalized = (sourcePlatform ?? '').toLowerCase()
    if (normalized === 'spotify') {
        return 'spotify'
    }
    if (normalized === 'tidal') {
        return 'tidal'
    }
    return 'idle'
}

const VisualizerPage = () => {
    const navigate = useNavigate()
    const { session } = useAuthSession()
    const {
        currentItem,
        isPlaying,
        isLoading,
        positionMs,
        durationMs,
        pause,
        resume,
        skipNext,
        skipPrevious,
    } = usePlayback()
    const spotifyTrackId = currentItem ? resolveSpotifyTrackId(currentItem) : null
    const spotifyAdapter = useSpotifyAudioAnalysisAdapter({
        userId: session?.userId ?? null,
        spotifyTrackId: currentItem?.sourcePlatform === 'spotify' ? spotifyTrackId : null,
        positionMs,
    })

    if (!currentItem) {
        return (
            <main className="space-y-6">
                <section className="rounded-2xl border border-hud-border-secondary bg-hud-bg-secondary/80 p-8 text-center">
                    <p className="text-xs uppercase tracking-[0.24em] text-hud-text-muted">Visualizer</p>
                    <h2 className="mt-3 text-xl font-semibold text-hud-text-primary">재생 중인 트랙이 없습니다</h2>
                    <p className="mt-2 text-sm text-hud-text-secondary">
                        EMS/PMS/GMS에서 트랙을 재생한 뒤 다시 들어오세요.
                    </p>
                    <Button type="button" variant="outline" className="mt-6" onClick={() => navigate(-1)}>
                        <ArrowLeft size={16} />
                        돌아가기
                    </Button>
                </section>
            </main>
        )
    }

    const mode = resolveMode(currentItem.sourcePlatform)
    const handleTogglePlayback = () => {
        if (isPlaying) {
            void pause()
        } else {
            void resume()
        }
    }

    return (
        <main className="space-y-6">
            <section className="rounded-2xl border border-hud-border-secondary bg-hud-bg-secondary/80 p-6">
                <div className="flex items-center justify-between gap-4">
                    <div className="flex items-center gap-3">
                        <Button type="button" variant="ghost" onClick={() => navigate(-1)} aria-label="뒤로">
                            <ArrowLeft size={18} />
                        </Button>
                        <div>
                            <p className="text-[11px] uppercase tracking-[0.24em] text-hud-text-muted">
                                Visualizer · {mode}
                            </p>
                            <h2 className="mt-1 text-lg font-semibold text-hud-text-primary">{currentItem.title}</h2>
                            <p className="mt-1 text-xs text-hud-text-secondary">{currentItem.subtitle}</p>
                        </div>
                    </div>
                </div>
            </section>

            <section className="flex flex-col items-center gap-6 rounded-2xl border border-hud-border-secondary bg-hud-bg-secondary/60 p-8">
                {currentItem.imageUrl ? (
                    <img
                        src={currentItem.imageUrl}
                        alt={currentItem.albumTitle ?? currentItem.title}
                        className="h-56 w-56 rounded-2xl object-cover shadow-lg"
                    />
                ) : (
                    <div className="flex h-56 w-56 items-center justify-center rounded-2xl bg-hud-bg-primary/60 text-hud-text-muted">
                        no cover
                    </div>
                )}

                <Visualizer
                    playing={isPlaying}
                    mode={mode}
                    bars={VISUALIZER_BARS}
                    heightsAt={mode === 'spotify' && spotifyAdapter.heightsAt ? spotifyAdapter.heightsAt : undefined}
                />

                <div className="w-full max-w-2xl">
                    <div className="flex items-center justify-between text-xs text-hud-text-muted">
                        <span>{formatDuration(positionMs)}</span>
                        <span>{formatDuration(durationMs)}</span>
                    </div>
                    <div className="mt-2 h-1 w-full overflow-hidden rounded-full bg-hud-bg-primary/70">
                        <div
                            className="h-full bg-hud-accent-primary transition-[width] duration-150 ease-linear"
                            style={{ width: `${durationMs > 0 ? Math.min(100, (positionMs / durationMs) * 100) : 0}%` }}
                        />
                    </div>
                </div>

                <div className="flex items-center gap-3">
                    <Button type="button" variant="ghost" onClick={() => void skipPrevious()} aria-label="이전">
                        <SkipBack size={20} />
                    </Button>
                    <Button
                        type="button"
                        variant="primary"
                        glow
                        size="lg"
                        onClick={handleTogglePlayback}
                        disabled={isLoading}
                        aria-label={isPlaying ? '일시정지' : '재생'}
                    >
                        {isPlaying ? <Pause size={22} /> : <Play size={22} />}
                    </Button>
                    <Button type="button" variant="ghost" onClick={() => void skipNext()} aria-label="다음">
                        <SkipForward size={20} />
                    </Button>
                </div>

                {mode === 'spotify' && spotifyAdapter.error && (
                    <div className="flex max-w-2xl items-start gap-3 rounded-xl border border-amber-300/40 bg-amber-300/10 p-3 text-xs text-amber-100">
                        <AlertTriangle size={14} className="mt-0.5 shrink-0" />
                        <p>
                            Spotify audio-analysis 어댑터가 작동하지 않아 procedural envelope로 fallback 중입니다.
                            <br />
                            <span className="text-amber-200/80">{spotifyAdapter.error}</span>
                        </p>
                    </div>
                )}
                <p className="text-[11px] text-hud-text-muted">
                    {mode === 'spotify' && spotifyAdapter.ready
                        ? '실시간 신호: Spotify audio-analysis segment + beat 기반 bar 매핑.'
                        : mode === 'tidal'
                            ? 'TIDAL AnalyserNode 어댑터는 후속 commit에서 연결됩니다. 현재는 mode 기반 procedural envelope.'
                            : 'mode 기반 procedural envelope로 동작합니다.'}
                </p>
            </section>
        </main>
    )
}

export default VisualizerPage
