import { Link } from 'react-router-dom'
import { ListMusic } from 'lucide-react'
import MusicArtwork from '@/components/music/MusicArtwork'
import { usePopularPlaylists } from '@/hooks/usePopularPlaylists'

const POPULAR_LIMIT = 6

const PopularPlaylistsSection = () => {
    const state = usePopularPlaylists(POPULAR_LIMIT)

    if (state.status === 'loading') {
        return (
            <section className="space-y-3">
                <header className="flex items-baseline justify-between">
                    <h2 className="text-lg font-semibold text-hud-text-primary">Popular playlists</h2>
                    <span className="text-xs text-hud-text-muted">Loading…</span>
                </header>
                <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
                    {Array.from({ length: POPULAR_LIMIT }).map((_, index) => (
                        <div
                            key={index}
                            className="aspect-square animate-pulse rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/60"
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
            <header className="flex items-baseline justify-between">
                <h2 className="text-lg font-semibold text-hud-text-primary">Popular playlists</h2>
                <span className="text-xs text-hud-text-muted">Top by track count</span>
            </header>
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
                {state.playlists.map((playlist) => {
                    const seed = `${playlist.source_platform}-${playlist.external_playlist_id}`
                    return (
                        <Link
                            key={playlist.playlist_id}
                            to={`/playlists/ems/${playlist.playlist_id}`}
                            className="group flex flex-col overflow-hidden rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 transition-hud hover:border-hud-border-primary hover:bg-hud-bg-primary/90"
                        >
                            <div className="relative aspect-square overflow-hidden">
                                <MusicArtwork
                                    imageUrl={playlist.cover_image_url}
                                    seed={seed}
                                    label={playlist.title}
                                />
                            </div>
                            <div className="space-y-1 p-3">
                                <p className="truncate text-sm font-semibold text-hud-text-primary">{playlist.title}</p>
                                <p className="truncate text-xs text-hud-text-secondary">
                                    {playlist.curator || playlist.source_platform.toUpperCase()}
                                </p>
                                <p className="flex items-center gap-1 text-[11px] text-hud-text-muted">
                                    <ListMusic size={12} />
                                    {playlist.track_count} tracks
                                </p>
                            </div>
                        </Link>
                    )
                })}
            </div>
        </section>
    )
}

export default PopularPlaylistsSection
