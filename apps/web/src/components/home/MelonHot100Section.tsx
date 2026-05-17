import { ArrowRight, ExternalLink } from 'lucide-react'
import { Link } from 'react-router-dom'
import MusicArtwork from '@/components/music/MusicArtwork'
import { useMelonHot100 } from '@/hooks/useMelonHot100'

const SECTION_LIMIT = 10

const MelonHot100Section = () => {
    const state = useMelonHot100(SECTION_LIMIT)

    if (state.status === 'loading') {
        return (
            <section className="space-y-3">
                <header className="flex items-baseline justify-between">
                    <h2 className="text-lg font-semibold text-hud-text-primary">Melon Hot 100</h2>
                    <span className="text-xs text-hud-text-muted">Loading…</span>
                </header>
                <div className="grid gap-2 sm:grid-cols-2">
                    {Array.from({ length: SECTION_LIMIT }).map((_, index) => (
                        <div
                            key={index}
                            className="h-14 animate-pulse rounded-xl border border-hud-border-secondary bg-hud-bg-primary/60"
                        />
                    ))}
                </div>
            </section>
        )
    }

    if (state.status === 'empty' || state.status === 'error') {
        return null
    }

    return (
        <section className="space-y-4">
            <header className="flex items-baseline justify-between gap-3">
                <div>
                    <h2 className="text-lg font-semibold text-hud-text-primary">Melon Hot 100</h2>
                    {state.snapshotAt && (
                        <p className="mt-0.5 text-[11px] text-hud-text-muted">
                            Snapshot {new Date(state.snapshotAt).toLocaleString()}
                        </p>
                    )}
                </div>
                <Link
                    to="/melon-hot-100"
                    className="inline-flex items-center gap-1 text-xs font-semibold text-hud-accent-primary transition-hud hover:text-hud-accent-primary/80"
                >
                    View all 100
                    <ArrowRight size={14} />
                </Link>
            </header>
            <ol className="grid gap-2 sm:grid-cols-2">
                {state.tracks.map((track) => (
                    <li key={`${track.rank}-${track.melon_song_id ?? track.title}`}>
                        <a
                            href={track.song_external_url ?? '#'}
                            target="_blank"
                            rel="noreferrer noopener"
                            className="group flex items-center gap-3 rounded-xl border border-hud-border-secondary bg-hud-bg-primary/70 px-3 py-2 transition-hud hover:border-hud-border-primary hover:bg-hud-bg-primary/90"
                        >
                            <span className="w-7 text-center font-mono text-xs text-hud-text-muted">
                                {track.rank}
                            </span>
                            <div className="h-10 w-10 shrink-0 overflow-hidden rounded-md border border-hud-border-secondary bg-hud-bg-primary">
                                <MusicArtwork
                                    imageUrl={track.image_url}
                                    seed={`melon-${track.rank}`}
                                    label={track.title}
                                />
                            </div>
                            <div className="min-w-0 flex-1">
                                <p className="truncate text-sm font-semibold text-hud-text-primary">{track.title}</p>
                                <p className="truncate text-xs text-hud-text-secondary">{track.artist_name}</p>
                            </div>
                            <ExternalLink size={14} className="text-hud-text-muted opacity-0 transition-opacity duration-150 group-hover:opacity-100" />
                        </a>
                    </li>
                ))}
            </ol>
        </section>
    )
}

export default MelonHot100Section
