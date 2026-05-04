import { ExternalLink, Play, Plus } from 'lucide-react'
import Button from '@/components/common/Button'
import MusicArtwork from '@/components/music/MusicArtwork'
import { formatDuration } from '@/lib/musicPlayback'

interface TrackFeatureCardProps {
    title: string
    artistName: string
    sourcePlatform: string
    albumTitle?: string | null
    imageUrl?: string | null
    durationMs?: number | null
    reason?: string | null
    badges?: string[]
    onPlay?: () => void
    onUseAsSeed?: () => void
    onOpenExternal?: () => void
}

const TrackFeatureCard = ({
    title,
    artistName,
    sourcePlatform,
    albumTitle,
    imageUrl,
    durationMs,
    reason,
    badges = [],
    onPlay,
    onUseAsSeed,
    onOpenExternal,
}: TrackFeatureCardProps) => {
    const durationLabel = formatDuration(durationMs)

    return (
        <div className="overflow-hidden rounded-[24px] border border-hud-border-secondary bg-hud-bg-primary/75">
            <div className="aspect-[4/3]">
                <MusicArtwork imageUrl={imageUrl} seed={`${artistName}-${title}`} label={`${title} ${artistName}`} />
            </div>
            <div className="space-y-4 p-4">
                <div>
                    <div className="flex flex-wrap gap-2">
                        <span className="rounded-full border border-hud-border-secondary px-3 py-1 text-[11px] uppercase tracking-[0.24em] text-hud-text-muted">
                            {sourcePlatform}
                        </span>
                        {durationLabel && (
                            <span className="rounded-full border border-hud-border-secondary px-3 py-1 text-[11px] uppercase tracking-[0.24em] text-hud-text-muted">
                                {durationLabel}
                            </span>
                        )}
                        {badges.map((badge) => (
                            <span
                                key={badge}
                                className="rounded-full border border-hud-border-primary bg-hud-accent-primary/10 px-3 py-1 text-[11px] uppercase tracking-[0.24em] text-hud-accent-primary"
                            >
                                {badge}
                            </span>
                        ))}
                    </div>
                    <h3 className="mt-3 text-lg font-semibold text-hud-text-primary">{title}</h3>
                    <p className="mt-1 text-sm text-hud-text-secondary">{artistName}</p>
                    {albumTitle && <p className="mt-2 text-xs uppercase tracking-[0.22em] text-hud-text-muted">{albumTitle}</p>}
                </div>

                {reason && <p className="text-sm leading-6 text-hud-text-secondary">{reason}</p>}

                <div className="flex flex-wrap gap-3">
                    {onPlay && (
                        <Button type="button" variant="primary" onClick={onPlay}>
                            <Play size={18} />
                            Play
                        </Button>
                    )}
                    {onUseAsSeed && (
                        <Button type="button" variant="outline" onClick={onUseAsSeed}>
                            <Plus size={18} />
                            Use as Seed
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
    )
}

export default TrackFeatureCard
