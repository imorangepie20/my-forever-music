import {
    ExternalLink,
    Loader2,
    Pause,
    Play,
    SkipBack,
    SkipForward,
    Volume2,
    X,
} from 'lucide-react'
import Button from '@/components/common/Button'
import MusicArtwork from '@/components/music/MusicArtwork'
import { usePlayback } from '@/contexts/PlaybackContext'
import { formatDuration, resolvePlaybackPlatformId } from '@/lib/musicPlayback'

interface PlaybackDockProps {
    sidebarCollapsed?: boolean
}

const PlaybackDock = ({ sidebarCollapsed = false }: PlaybackDockProps) => {
    const {
        currentItem,
        queue,
        currentIndex,
        isPlaying,
        isLoading,
        error,
        notice,
        positionMs,
        durationMs,
        volume,
        pause,
        resume,
        skipNext,
        skipPrevious,
        seek,
        setVolume,
        clearItem,
    } = usePlayback()

    if (!currentItem) {
        return null
    }

    const playbackPlatformId = resolvePlaybackPlatformId(currentItem)
    const totalDuration = durationMs || currentItem.durationMs || 0
    const progressValue = totalDuration > 0 ? Math.min(positionMs, totalDuration) : 0

    const handleTogglePlayback = () => {
        if (isPlaying) {
            void pause()
            return
        }
        void resume()
    }

    return (
        <div className={`fixed bottom-0 right-0 z-40 border-t border-hud-border-secondary bg-hud-bg-secondary/95 backdrop-blur-xl transition-all duration-300 ${sidebarCollapsed ? 'lg:left-24' : 'lg:left-72'}`}>
            <div className="mx-auto grid max-w-[1600px] gap-4 px-4 py-4 lg:grid-cols-[minmax(280px,420px)_minmax(0,1fr)_minmax(220px,300px)] lg:px-8">
                <div className="flex min-w-0 items-center gap-4">
                    <div className="h-16 w-16 shrink-0 overflow-hidden rounded-lg">
                        <MusicArtwork
                            imageUrl={currentItem.imageUrl}
                            seed={`${currentItem.sourcePlatform}-${currentItem.title}`}
                            label={currentItem.title}
                        />
                    </div>
                    <div className="min-w-0">
                        <p className="text-[11px] uppercase tracking-[0.24em] text-hud-text-muted">
                            {currentItem.kind} · {playbackPlatformId ?? currentItem.sourcePlatform}
                        </p>
                        <h3 className="mt-1 truncate text-base font-semibold text-hud-text-primary">
                            {currentItem.title}
                        </h3>
                        <p className="mt-0.5 truncate text-sm text-hud-text-secondary">{currentItem.subtitle}</p>
                    </div>
                </div>

                <div className="min-w-0">
                    <div className="flex items-center justify-center gap-3">
                        <button
                            type="button"
                            onClick={() => void skipPrevious()}
                            className="flex h-10 w-10 items-center justify-center rounded-full border border-hud-border-secondary text-hud-text-secondary transition-hud hover:border-hud-border-primary hover:text-hud-text-primary"
                            aria-label="Previous track"
                        >
                            <SkipBack size={18} />
                        </button>
                        <button
                            type="button"
                            onClick={handleTogglePlayback}
                            disabled={isLoading}
                            className="flex h-12 w-12 items-center justify-center rounded-full bg-hud-accent-primary text-hud-bg-primary transition-hud hover:bg-hud-accent-primary/90 disabled:cursor-not-allowed disabled:opacity-60"
                            aria-label={isPlaying ? 'Pause playback' : 'Resume playback'}
                        >
                            {isLoading ? (
                                <Loader2 size={22} className="animate-spin" />
                            ) : isPlaying ? (
                                <Pause size={22} />
                            ) : (
                                <Play size={22} className="translate-x-0.5" />
                            )}
                        </button>
                        <button
                            type="button"
                            onClick={() => void skipNext()}
                            className="flex h-10 w-10 items-center justify-center rounded-full border border-hud-border-secondary text-hud-text-secondary transition-hud hover:border-hud-border-primary hover:text-hud-text-primary"
                            aria-label="Next track"
                        >
                            <SkipForward size={18} />
                        </button>
                    </div>

                    <div className="mt-3 grid grid-cols-[48px_minmax(0,1fr)_48px] items-center gap-3">
                        <span className="text-right text-xs text-hud-text-muted">{formatDuration(progressValue)}</span>
                        <input
                            type="range"
                            min={0}
                            max={Math.max(totalDuration, 1)}
                            value={progressValue}
                            onChange={(event) => void seek(Number(event.target.value))}
                            className="h-1 w-full accent-hud-accent-primary"
                        />
                        <span className="text-xs text-hud-text-muted">{formatDuration(totalDuration)}</span>
                    </div>

                    {(error || notice || isLoading) && (
                        <p className={`mt-2 flex items-center justify-center gap-2 truncate text-center text-xs font-medium ${error ? 'text-amber-300' : 'text-hud-accent-primary'}`}>
                            {isLoading && !error && <Loader2 size={13} className="shrink-0 animate-spin" />}
                            <span className="truncate">{error ?? notice ?? 'Preparing playback...'}</span>
                        </p>
                    )}
                </div>

                <div className="flex items-center justify-end gap-3">
                    <span className="rounded-full border border-hud-border-secondary px-3 py-2 text-xs uppercase tracking-[0.2em] text-hud-text-muted">
                        Queue {queue.length > 0 ? `${currentIndex + 1}/${queue.length}` : '0/0'}
                    </span>
                    <Volume2 size={18} className="text-hud-text-secondary" />
                    <input
                        type="range"
                        min={0}
                        max={1}
                        step={0.01}
                        value={volume}
                        onChange={(event) => void setVolume(Number(event.target.value))}
                        className="w-24 accent-hud-accent-primary"
                    />
                    {currentItem.externalUrl && (
                        <Button
                            type="button"
                            variant="ghost"
                            onClick={() => window.open(currentItem.externalUrl ?? undefined, '_blank', 'noopener,noreferrer')}
                            aria-label="Open in platform"
                        >
                            <ExternalLink size={18} />
                        </Button>
                    )}
                    <Button type="button" variant="ghost" onClick={clearItem} aria-label="Close player">
                        <X size={18} />
                    </Button>
                </div>
            </div>
        </div>
    )
}

export default PlaybackDock
