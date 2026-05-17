import { ArrowRight, ListMusic, Sparkles } from 'lucide-react'
import { Link } from 'react-router-dom'
import MusicArtwork from '@/components/music/MusicArtwork'
import { useAuthSession } from '@/contexts/AuthSessionContext'
import { useGmsRecommendedPlaylists } from '@/hooks/useGmsRecommendedPlaylists'

const RECOMMENDED_LIMIT = 5

const GmsRecommendedPlaylistsSection = () => {
    const { session } = useAuthSession()
    const state = useGmsRecommendedPlaylists(session?.userId ?? null, RECOMMENDED_LIMIT)

    if (state.status === 'loading') {
        return (
            <section className="space-y-3">
                <header className="flex items-baseline justify-between">
                    <h2 className="text-lg font-semibold text-hud-text-primary">Recommended for you</h2>
                    <span className="text-xs text-hud-text-muted">Loading…</span>
                </header>
                <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
                    {Array.from({ length: RECOMMENDED_LIMIT }).map((_, index) => (
                        <div
                            key={index}
                            className="aspect-square animate-pulse rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/60"
                        />
                    ))}
                </div>
            </section>
        )
    }

    if (state.status === 'anonymous') {
        return (
            <section className="space-y-3">
                <header className="flex items-baseline justify-between">
                    <h2 className="text-lg font-semibold text-hud-text-primary">Recommended for you</h2>
                </header>
                <div className="flex items-center justify-between gap-4 rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 px-6 py-5">
                    <div className="flex items-center gap-3">
                        <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-hud-accent-primary/10 text-hud-accent-primary">
                            <Sparkles size={20} />
                        </span>
                        <div>
                            <p className="text-sm font-semibold text-hud-text-primary">Sign in for personalized picks</p>
                            <p className="text-xs text-hud-text-secondary">
                                Connect your taste profile to unlock GMS playlist recommendations.
                            </p>
                        </div>
                    </div>
                    <Link
                        to="/signin"
                        className="inline-flex items-center gap-2 rounded-full bg-hud-accent-primary px-4 py-2 text-sm font-semibold text-hud-bg-primary transition-hud hover:bg-hud-accent-primary/90"
                    >
                        Sign in
                        <ArrowRight size={14} />
                    </Link>
                </div>
            </section>
        )
    }

    if (state.status === 'cold_start') {
        return (
            <section className="space-y-3">
                <header className="flex items-baseline justify-between">
                    <h2 className="text-lg font-semibold text-hud-text-primary">Recommended for you</h2>
                </header>
                <div className="flex items-center justify-between gap-4 rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 px-6 py-5">
                    <div className="flex items-center gap-3">
                        <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-hud-accent-info/10 text-hud-accent-info">
                            <ListMusic size={20} />
                        </span>
                        <div>
                            <p className="text-sm font-semibold text-hud-text-primary">Build your taste library first</p>
                            <p className="text-xs text-hud-text-secondary">
                                Import playlists from your streaming services so GMS can rank picks for you.
                            </p>
                        </div>
                    </div>
                    <Link
                        to="/platforms"
                        className="inline-flex items-center gap-2 rounded-full border border-hud-border-secondary px-4 py-2 text-sm font-semibold text-hud-text-primary transition-hud hover:border-hud-border-primary"
                    >
                        Connect platforms
                        <ArrowRight size={14} />
                    </Link>
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
                <h2 className="text-lg font-semibold text-hud-text-primary">Recommended for you</h2>
                <span className="text-xs text-hud-text-muted">GMS · top {state.playlists.length}</span>
            </header>
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
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
                                    <Sparkles size={12} className="text-hud-accent-primary" />
                                    {(playlist.composite_score * 100).toFixed(0)} match
                                </p>
                            </div>
                        </Link>
                    )
                })}
            </div>
        </section>
    )
}

export default GmsRecommendedPlaylistsSection
