import {
    AudioLines,
    ExternalLink,
    ListMusic,
    Loader2,
    Pause,
    Play,
    Repeat,
    Repeat1,
    Shuffle,
    SkipBack,
    SkipForward,
    Volume2,
    X,
} from 'lucide-react'
import type { ReactNode } from 'react'
import Button from '@/components/common/Button'
import MusicArtwork from '@/components/music/MusicArtwork'
import { usePlayback } from '@/contexts/PlaybackContext'
import { formatDuration, resolvePlaybackPlatformId } from '@/lib/musicPlayback'

interface PlaybackDockProps {
    sidebarCollapsed?: boolean
}

interface ControlButtonProps {
    children: ReactNode
    label: string
    active?: boolean
    disabled?: boolean
    primary?: boolean
    onClick: () => void
}

const qualityTone = (label: string | null) => {
    const normalized = label?.toLowerCase() ?? ''
    if (normalized.includes('lossless') || normalized.includes('flac') || normalized.includes('master')) {
        return 'border-hud-accent-primary/40 bg-hud-accent-primary/10 text-hud-accent-primary'
    }
    if (normalized.includes('spotify')) {
        return 'border-hud-accent-success/40 bg-hud-accent-success/10 text-hud-accent-success'
    }
    return 'border-hud-border-secondary bg-white/[0.03] text-hud-text-secondary'
}

const formatQualityParts = (label: string | null, platformId?: string | null) => {
    if (!label) {
        return {
            title: 'Pending',
            detail: 'Stream quality',
        }
    }

    const parts = label.split(' · ').map((part) => part.trim()).filter(Boolean)
    if (parts.length <= 1) {
        return {
            title: parts[0] ?? platformId?.toUpperCase() ?? 'Quality',
            detail: platformId ? `${platformId.toUpperCase()} stream` : 'Platform stream',
        }
    }

    return {
        title: parts[0],
        detail: parts.slice(1).join(' / '),
    }
}

const repeatShortLabel = (repeatMode: string) =>
    repeatMode === 'one' ? 'One' : repeatMode === 'all' ? 'All' : 'Off'

const ControlButton = ({ children, label, active = false, disabled = false, primary = false, onClick }: ControlButtonProps) => (
    <button
        type="button"
        onClick={onClick}
        disabled={disabled}
        className={`relative flex items-center justify-center rounded-full border transition-hud disabled:cursor-not-allowed disabled:opacity-50 ${
            primary
                ? 'h-12 w-12 border-hud-accent-primary/50 bg-hud-accent-primary text-hud-bg-primary shadow-hud-glow hover:bg-hud-accent-primary/90'
                : active
                    ? 'h-10 w-10 border-hud-accent-primary/50 bg-hud-accent-primary/10 text-hud-accent-primary'
                    : 'h-10 w-10 border-hud-border-secondary bg-white/[0.02] text-hud-text-secondary hover:border-hud-border-primary hover:bg-white/[0.05] hover:text-hud-text-primary'
        }`}
        aria-label={label}
        title={label}
    >
        {children}
        {active && !primary && <span className="absolute -bottom-1 h-1 w-1 rounded-full bg-hud-accent-primary" />}
    </button>
)

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
        shuffleEnabled,
        repeatMode,
        audioQualityLabel,
        pause,
        resume,
        skipNext,
        skipPrevious,
        seek,
        setVolume,
        toggleShuffle,
        cycleRepeatMode,
        clearItem,
    } = usePlayback()

    if (!currentItem) {
        return null
    }

    const playbackPlatformId = resolvePlaybackPlatformId(currentItem)
    const totalDuration = durationMs || currentItem.durationMs || 0
    const progressValue = totalDuration > 0 ? Math.min(positionMs, totalDuration) : 0
    const repeatLabel = repeatMode === 'one' ? 'Repeat one' : repeatMode === 'all' ? 'Repeat queue' : 'Repeat off'
    const RepeatIcon = repeatMode === 'one' ? Repeat1 : Repeat
    const qualityParts = formatQualityParts(audioQualityLabel, playbackPlatformId)
    const qualityClassName = qualityTone(audioQualityLabel)

    const handleTogglePlayback = () => {
        if (isPlaying) {
            void pause()
            return
        }
        void resume()
    }

    return (
        <div className={`fixed bottom-0 right-0 z-40 border-t border-hud-border-secondary bg-[rgba(11,18,32,0.96)] shadow-[0_-18px_55px_rgba(0,0,0,0.38)] backdrop-blur-xl transition-all duration-300 ${sidebarCollapsed ? 'lg:left-24' : 'lg:left-72'}`}>
            <div className="mx-auto grid max-w-[1600px] gap-4 px-4 py-4 lg:grid-cols-[minmax(260px,390px)_minmax(360px,1fr)_minmax(330px,430px)] lg:px-8">
                <div className="flex min-w-0 items-center gap-4">
                    <div className="h-16 w-16 shrink-0 overflow-hidden rounded-lg border border-hud-border-secondary bg-hud-bg-primary shadow-hud">
                        <MusicArtwork
                            imageUrl={currentItem.imageUrl}
                            seed={`${currentItem.sourcePlatform}-${currentItem.title}`}
                            label={currentItem.title}
                        />
                    </div>
                    <div className="min-w-0">
                        <p className="text-xs text-hud-text-muted">
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
                        <ControlButton
                            onClick={() => void toggleShuffle()}
                            active={shuffleEnabled}
                            label={shuffleEnabled ? 'Shuffle on' : 'Shuffle off'}
                        >
                            <Shuffle size={17} />
                        </ControlButton>
                        <ControlButton
                            onClick={() => void skipPrevious()}
                            label="Previous track"
                        >
                            <SkipBack size={18} />
                        </ControlButton>
                        <ControlButton
                            onClick={handleTogglePlayback}
                            disabled={isLoading}
                            primary
                            label={isPlaying ? 'Pause playback' : 'Resume playback'}
                        >
                            {isLoading ? (
                                <Loader2 size={22} className="animate-spin" />
                            ) : isPlaying ? (
                                <Pause size={22} />
                            ) : (
                                <Play size={22} className="translate-x-0.5" />
                            )}
                        </ControlButton>
                        <ControlButton
                            onClick={() => void skipNext()}
                            label="Next track"
                        >
                            <SkipForward size={18} />
                        </ControlButton>
                        <ControlButton
                            onClick={() => void cycleRepeatMode()}
                            active={repeatMode !== 'off'}
                            label={repeatLabel}
                        >
                            <RepeatIcon size={17} />
                            {repeatMode !== 'off' && (
                                <span className="absolute -right-2 -top-1 rounded-full border border-hud-border-secondary bg-hud-bg-primary px-1.5 text-[10px] font-semibold leading-4 text-hud-accent-primary">
                                    {repeatShortLabel(repeatMode)}
                                </span>
                            )}
                        </ControlButton>
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

                <div className="grid min-w-0 gap-3 sm:grid-cols-[minmax(0,1fr)_auto] lg:grid-cols-[minmax(0,1fr)_auto]">
                    <div className={`flex min-w-0 items-center gap-3 rounded-lg border px-3 py-2 ${qualityClassName}`} title={audioQualityLabel ?? undefined}>
                        <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border border-current/20 bg-black/10">
                            <AudioLines size={18} />
                        </span>
                        <span className="min-w-0">
                            <span className="block truncate text-sm font-semibold leading-5">{qualityParts.title}</span>
                            <span className="block truncate text-xs leading-4 text-hud-text-muted">{qualityParts.detail}</span>
                        </span>
                    </div>

                    <div className="flex items-center justify-end gap-2">
                        <div className="inline-flex h-11 items-center gap-2 rounded-lg border border-hud-border-secondary bg-white/[0.03] px-3 text-sm text-hud-text-secondary">
                            <ListMusic size={16} className="text-hud-accent-primary" />
                            <span className="font-semibold text-hud-text-primary">{queue.length > 0 ? `${currentIndex + 1}/${queue.length}` : '0/0'}</span>
                        </div>
                        <div className="hidden items-center gap-2 rounded-lg border border-hud-border-secondary bg-white/[0.03] px-3 py-2 xl:flex">
                            <Volume2 size={17} className="text-hud-text-secondary" />
                            <input
                                type="range"
                                min={0}
                                max={1}
                                step={0.01}
                                value={volume}
                                onChange={(event) => void setVolume(Number(event.target.value))}
                                className="w-20 accent-hud-accent-primary"
                                aria-label="Playback volume"
                            />
                        </div>
                        {currentItem.externalUrl && (
                            <Button
                                type="button"
                                variant="ghost"
                                onClick={() => window.open(currentItem.externalUrl ?? undefined, '_blank', 'noopener,noreferrer')}
                                aria-label="Open in platform"
                                className="h-11 w-11 px-0"
                            >
                                <ExternalLink size={18} />
                            </Button>
                        )}
                        <Button type="button" variant="ghost" onClick={clearItem} aria-label="Close player" className="h-11 w-11 px-0">
                            <X size={18} />
                        </Button>
                    </div>
                </div>
            </div>
        </div>
    )
}

export default PlaybackDock
