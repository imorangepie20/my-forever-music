import { ExternalLink, Music4, Play } from 'lucide-react'
import Button from '@/components/common/Button'
import MusicArtwork from '@/components/music/MusicArtwork'

interface PlaylistFeatureCardProps {
    title: string
    sourcePlatform: string
    curator: string
    trackCount: number
    description: string
    imageUrl?: string | null
    isActive?: boolean
    onSelect?: () => void
    onPlay?: () => void
    onOpenExternal?: () => void
    actionLabel?: string
}

const PlaylistFeatureCard = ({
    title,
    sourcePlatform,
    curator,
    trackCount,
    description,
    imageUrl,
    isActive = false,
    onSelect,
    onPlay,
    onOpenExternal,
    actionLabel = 'Use Playlist',
}: PlaylistFeatureCardProps) => (
    <div
        className={`overflow-hidden rounded-[28px] border transition-hud ${
            isActive
                ? 'border-hud-border-primary bg-hud-accent-primary/10 shadow-[0_0_0_1px_rgba(87,172,255,0.18)]'
                : 'border-hud-border-secondary bg-hud-bg-primary/75'
        }`}
    >
        <div className="grid gap-0 md:grid-cols-[180px_minmax(0,1fr)]">
            <div className="h-44 md:h-full">
                <MusicArtwork imageUrl={imageUrl} seed={`${sourcePlatform}-${title}`} label={title} />
            </div>
            <div className="space-y-4 p-5">
                <div className="flex flex-wrap items-start justify-between gap-3">
                    <div>
                        <div className="flex flex-wrap gap-2">
                            <span className="rounded-full border border-hud-border-secondary px-3 py-1 text-[11px] uppercase tracking-[0.24em] text-hud-text-muted">
                                {sourcePlatform}
                            </span>
                            <span className="rounded-full border border-hud-border-secondary px-3 py-1 text-[11px] uppercase tracking-[0.24em] text-hud-text-muted">
                                {trackCount} tracks
                            </span>
                        </div>
                        <h3 className="mt-3 text-xl font-semibold text-hud-text-primary">{title}</h3>
                        <p className="mt-2 text-sm text-hud-text-secondary">Curated by {curator}</p>
                    </div>
                    <span className="rounded-2xl bg-hud-bg-secondary/80 px-4 py-3 text-right">
                        <span className="block text-[11px] uppercase tracking-[0.24em] text-hud-text-muted">Status</span>
                        <span className="mt-1 block text-sm font-medium text-hud-text-primary">
                            {isActive ? 'Selected' : 'Available'}
                        </span>
                    </span>
                </div>

                <p className="text-sm leading-6 text-hud-text-secondary">{description}</p>

                <div className="flex flex-wrap gap-3">
                    {onSelect && (
                        <Button type="button" variant={isActive ? 'primary' : 'ghost'} onClick={onSelect}>
                            <Music4 size={18} />
                            {actionLabel}
                        </Button>
                    )}
                    {onPlay && (
                        <Button type="button" variant="outline" onClick={onPlay}>
                            <Play size={18} />
                            Play
                        </Button>
                    )}
                    {onOpenExternal && (
                        <Button type="button" variant="ghost" onClick={onOpenExternal}>
                            <ExternalLink size={18} />
                            Open
                        </Button>
                    )}
                </div>
            </div>
        </div>
    </div>
)

export default PlaylistFeatureCard
