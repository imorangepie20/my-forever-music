import { ArrowLeft, ExternalLink } from 'lucide-react'
import { Link } from 'react-router-dom'
import MusicArtwork from '@/components/music/MusicArtwork'
import HudCard from '@/components/common/HudCard'
import { useMelonHot100 } from '@/hooks/useMelonHot100'

const MelonHot100Page = () => {
    const state = useMelonHot100(null, true)

    return (
        <div className="space-y-5">
            <header className="flex items-center justify-between gap-3">
                <Link
                    to="/"
                    className="inline-flex items-center gap-2 text-sm text-hud-text-secondary transition-hud hover:text-hud-text-primary"
                >
                    <ArrowLeft size={16} />
                    Back to home
                </Link>
                {state.status === 'ready' && state.snapshotAt && (
                    <span className="text-xs text-hud-text-muted">
                        Snapshot {new Date(state.snapshotAt).toLocaleString()}
                    </span>
                )}
            </header>

            <HudCard
                title="Melon Hot 100"
                subtitle="Live snapshot scraped from https://www.melon.com/chart/index.htm"
            >
                {state.status === 'loading' && (
                    <p className="text-sm text-hud-text-secondary">Loading chart…</p>
                )}
                {state.status === 'error' && (
                    <p className="text-sm text-amber-300">{state.error}</p>
                )}
                {state.status === 'empty' && (
                    <p className="text-sm text-hud-text-secondary">
                        No Melon chart data yet. Trigger a scrape from the admin endpoint.
                    </p>
                )}
                {state.status === 'ready' && (
                    <ol className="divide-y divide-hud-border-secondary">
                        {state.tracks.map((track) => (
                            <li key={`${track.rank}-${track.melon_song_id ?? track.title}`}>
                                <a
                                    href={track.song_external_url ?? '#'}
                                    target="_blank"
                                    rel="noreferrer noopener"
                                    className="group flex items-center gap-3 py-2 transition-hud hover:bg-hud-bg-primary/60"
                                >
                                    <span className="w-10 text-center font-mono text-sm text-hud-text-muted">
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
                                        <p className="truncate text-xs text-hud-text-secondary">
                                            {track.artist_name}
                                            {track.album_title ? <span className="text-hud-text-muted"> · {track.album_title}</span> : null}
                                        </p>
                                    </div>
                                    <ExternalLink size={14} className="text-hud-text-muted opacity-0 transition-opacity duration-150 group-hover:opacity-100" />
                                </a>
                            </li>
                        ))}
                    </ol>
                )}
            </HudCard>
        </div>
    )
}

export default MelonHot100Page
