import { startTransition, useEffect, useState } from 'react'
import { LibraryBig, ListMusic, RefreshCw, Sparkles, UserRound } from 'lucide-react'
import { Link } from 'react-router-dom'
import Button from '@/components/common/Button'
import HudCard from '@/components/common/HudCard'
import { ApiError, fetchPmsWorkspaceBootstrap } from '@/services/api'
import { useRecommendationWorkspace } from '@/contexts/RecommendationWorkspaceContext'
import type { PmsWorkspaceBootstrapResponse } from '@/types/api'

const splitItems = (value: string) =>
    value
        .split(',')
        .map((item) => item.trim())
        .filter(Boolean)

const mergeCsv = (current: string, nextValue: string) => {
    const merged = [...splitItems(current), nextValue]
    return Array.from(new Set(merged)).join(', ')
}

const PmsPage = () => {
    const {
        workspace,
        updateWorkspace,
        resetWorkspace,
        seedTrackCount,
        seedArtistCount,
        seedGenreCount,
    } = useRecommendationWorkspace()
    const [bootstrap, setBootstrap] = useState<PmsWorkspaceBootstrapResponse | null>(null)
    const [isLoading, setIsLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        const controller = new AbortController()

        setIsLoading(true)
        setError(null)

        fetchPmsWorkspaceBootstrap(controller.signal)
            .then((response) => {
                startTransition(() => {
                    setBootstrap(response)
                    setError(null)
                })
            })
            .catch((requestError: unknown) => {
                if (requestError instanceof DOMException && requestError.name === 'AbortError') {
                    return
                }

                const message =
                    requestError instanceof ApiError
                        ? requestError.message
                        : 'Unable to load PMS bootstrap data from the Spring Boot API.'

                startTransition(() => {
                    setError(message)
                })
            })
            .finally(() => {
                setIsLoading(false)
            })

        return () => controller.abort()
    }, [])

    const applyBootstrapDefaults = () => {
        if (!bootstrap) {
            return
        }

        updateWorkspace({
            userId: bootstrap.workspace_defaults.user_id,
            playlistId: bootstrap.workspace_defaults.playlist_id,
            seedTrackIdsText: bootstrap.workspace_defaults.seed_track_ids.join(', '),
            seedArtistNamesText: bootstrap.workspace_defaults.seed_artist_names.join(', '),
            seedGenresText: bootstrap.workspace_defaults.seed_genres.join(', '),
        })
    }

    return (
        <div className="space-y-6">
            <section className="grid gap-6 xl:grid-cols-[1.15fr_0.85fr]">
                <HudCard title="PMS Seed Workspace" subtitle="Collect playlist and catalog seeds before recommendation">
                    <div className="grid gap-5 md:grid-cols-2">
                        <div>
                            <label className="mb-2 block text-sm font-medium text-hud-text-secondary">User ID</label>
                            <input
                                value={workspace.userId}
                                onChange={(event) => updateWorkspace({ userId: event.target.value })}
                                className="w-full rounded-xl border border-hud-border-secondary bg-hud-bg-primary px-4 py-3 text-sm text-hud-text-primary outline-none transition-hud focus:border-hud-border-primary"
                            />
                        </div>
                        <div>
                            <label className="mb-2 block text-sm font-medium text-hud-text-secondary">Playlist ID</label>
                            <input
                                value={workspace.playlistId}
                                onChange={(event) => updateWorkspace({ playlistId: event.target.value })}
                                className="w-full rounded-xl border border-hud-border-secondary bg-hud-bg-primary px-4 py-3 text-sm text-hud-text-primary outline-none transition-hud focus:border-hud-border-primary"
                            />
                        </div>
                    </div>

                    <div className="mt-5 space-y-5">
                        <div>
                            <label className="mb-2 block text-sm font-medium text-hud-text-secondary">
                                Seed Track IDs
                            </label>
                            <textarea
                                value={workspace.seedTrackIdsText}
                                onChange={(event) => updateWorkspace({ seedTrackIdsText: event.target.value })}
                                rows={4}
                                className="w-full rounded-2xl border border-hud-border-secondary bg-hud-bg-primary px-4 py-3 text-sm text-hud-text-primary outline-none transition-hud focus:border-hud-border-primary"
                            />
                        </div>

                        <div>
                            <label className="mb-2 block text-sm font-medium text-hud-text-secondary">
                                Seed Artist Names
                            </label>
                            <textarea
                                value={workspace.seedArtistNamesText}
                                onChange={(event) => updateWorkspace({ seedArtistNamesText: event.target.value })}
                                rows={3}
                                className="w-full rounded-2xl border border-hud-border-secondary bg-hud-bg-primary px-4 py-3 text-sm text-hud-text-primary outline-none transition-hud focus:border-hud-border-primary"
                            />
                        </div>

                        <div>
                            <label className="mb-2 block text-sm font-medium text-hud-text-secondary">
                                Seed Genres
                            </label>
                            <textarea
                                value={workspace.seedGenresText}
                                onChange={(event) => updateWorkspace({ seedGenresText: event.target.value })}
                                rows={3}
                                className="w-full rounded-2xl border border-hud-border-secondary bg-hud-bg-primary px-4 py-3 text-sm text-hud-text-primary outline-none transition-hud focus:border-hud-border-primary"
                            />
                        </div>
                    </div>

                    <div className="mt-6 flex flex-wrap gap-3">
                        <Button type="button" variant="ghost" onClick={applyBootstrapDefaults} disabled={!bootstrap}>
                            Use API Defaults
                        </Button>
                        <Button type="button" variant="outline" onClick={resetWorkspace}>
                            Reset Workspace
                        </Button>
                        <Link to="/ems">
                            <Button type="button" variant="primary" glow>
                                Continue to EMS
                            </Button>
                        </Link>
                    </div>
                </HudCard>

                <div className="space-y-6">
                    <HudCard title="Seed Summary" subtitle="What will feed the recommendation pipeline">
                        <div className="space-y-4">
                            <div className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                                <div className="flex items-center gap-3">
                                    <span className="rounded-xl bg-hud-accent-primary/10 p-2 text-hud-accent-primary">
                                        <ListMusic size={18} />
                                    </span>
                                    <div>
                                        <p className="text-xs uppercase tracking-[0.22em] text-hud-text-muted">
                                            Track Seeds
                                        </p>
                                        <p className="mt-1 text-2xl font-semibold text-hud-text-primary">
                                            {seedTrackCount}
                                        </p>
                                    </div>
                                </div>
                            </div>
                            <div className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                                <div className="flex items-center gap-3">
                                    <span className="rounded-xl bg-hud-accent-info/10 p-2 text-hud-accent-info">
                                        <UserRound size={18} />
                                    </span>
                                    <div>
                                        <p className="text-xs uppercase tracking-[0.22em] text-hud-text-muted">
                                            Artist Seeds
                                        </p>
                                        <p className="mt-1 text-2xl font-semibold text-hud-text-primary">
                                            {seedArtistCount}
                                        </p>
                                    </div>
                                </div>
                            </div>
                            <div className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                                <div className="flex items-center gap-3">
                                    <span className="rounded-xl bg-hud-accent-secondary/10 p-2 text-hud-accent-secondary">
                                        <Sparkles size={18} />
                                    </span>
                                    <div>
                                        <p className="text-xs uppercase tracking-[0.22em] text-hud-text-muted">
                                            Genre Seeds
                                        </p>
                                        <p className="mt-1 text-2xl font-semibold text-hud-text-primary">
                                            {seedGenreCount}
                                        </p>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </HudCard>

                    <HudCard
                        title="PMS Bootstrap Feed"
                        subtitle="Spring Boot workspace suggestions"
                        action={
                            isLoading ? (
                                <span className="inline-flex items-center gap-2 text-xs text-hud-text-muted">
                                    <RefreshCw size={14} className="animate-spin" />
                                    Loading
                                </span>
                            ) : null
                        }
                    >
                        {error ? (
                            <div className="rounded-2xl border border-hud-accent-danger/40 bg-hud-accent-danger/10 p-4 text-sm leading-6 text-hud-text-secondary">
                                {error}
                            </div>
                        ) : bootstrap ? (
                            <div className="space-y-4">
                                <div className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                                    <div className="flex items-center gap-3">
                                        <span className="rounded-xl bg-hud-accent-primary/10 p-2 text-hud-accent-primary">
                                            <LibraryBig size={18} />
                                        </span>
                                        <div>
                                            <p className="text-xs uppercase tracking-[0.22em] text-hud-text-muted">
                                                Bootstrap Generated
                                            </p>
                                            <p className="mt-1 text-sm text-hud-text-primary">
                                                {new Date(bootstrap.generated_at).toLocaleString()}
                                            </p>
                                        </div>
                                    </div>
                                </div>

                                <div>
                                    <p className="mb-3 text-xs font-semibold uppercase tracking-[0.22em] text-hud-text-muted">
                                        Playlist Options
                                    </p>
                                    <div className="space-y-3">
                                        {bootstrap.playlists.map((playlist) => (
                                            <button
                                                key={playlist.playlist_id}
                                                type="button"
                                                onClick={() => updateWorkspace({ playlistId: playlist.playlist_id })}
                                                className={`w-full rounded-2xl border p-4 text-left transition-hud ${
                                                    workspace.playlistId === playlist.playlist_id
                                                        ? 'border-hud-border-primary bg-hud-accent-primary/10'
                                                        : 'border-hud-border-secondary bg-hud-bg-primary/70 hover:border-hud-border-primary'
                                                }`}
                                            >
                                                <div className="flex flex-wrap items-center justify-between gap-3">
                                                    <div>
                                                        <p className="text-sm font-semibold text-hud-text-primary">
                                                            {playlist.title}
                                                        </p>
                                                        <p className="mt-1 text-xs uppercase tracking-[0.2em] text-hud-text-muted">
                                                            {playlist.source_platform} · {playlist.track_count} tracks · {playlist.curator}
                                                        </p>
                                                    </div>
                                                    <span className="rounded-full border border-hud-border-secondary px-3 py-1 text-xs text-hud-text-secondary">
                                                        {playlist.playlist_id}
                                                    </span>
                                                </div>
                                                <p className="mt-3 text-sm leading-6 text-hud-text-secondary">
                                                    {playlist.highlight}
                                                </p>
                                            </button>
                                        ))}
                                    </div>
                                </div>

                                <div>
                                    <p className="mb-3 text-xs font-semibold uppercase tracking-[0.22em] text-hud-text-muted">
                                        Suggested Tracks
                                    </p>
                                    <div className="flex flex-wrap gap-2">
                                        {bootstrap.suggested_tracks.map((track) => (
                                            <button
                                                key={track.track_id}
                                                type="button"
                                                onClick={() =>
                                                    updateWorkspace({
                                                        seedTrackIdsText: mergeCsv(
                                                            workspace.seedTrackIdsText,
                                                            track.track_id,
                                                        ),
                                                    })
                                                }
                                                className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 px-3 py-2.5 text-left text-xs text-hud-text-secondary transition-hud hover:border-hud-border-primary hover:text-hud-text-primary"
                                            >
                                                <span className="block text-hud-text-primary">
                                                    {track.title} · {track.artist_name}
                                                </span>
                                                <span className="mt-1 block text-[11px] uppercase tracking-[0.18em] text-hud-text-muted">
                                                    {track.spotify_audio_features_filled
                                                        ? `spotify features ready · ${track.spotify_audio_feature_source}`
                                                        : 'spotify features pending'}
                                                </span>
                                            </button>
                                        ))}
                                    </div>
                                </div>

                                <div>
                                    <p className="mb-3 text-xs font-semibold uppercase tracking-[0.22em] text-hud-text-muted">
                                        Suggested Artists
                                    </p>
                                    <div className="space-y-2">
                                        {bootstrap.suggested_artists.map((artist) => (
                                            <button
                                                key={artist.artist_name}
                                                type="button"
                                                onClick={() =>
                                                    updateWorkspace({
                                                        seedArtistNamesText: mergeCsv(
                                                            workspace.seedArtistNamesText,
                                                            artist.artist_name,
                                                        ),
                                                    })
                                                }
                                                className="w-full rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 px-4 py-3 text-left transition-hud hover:border-hud-border-primary"
                                            >
                                                <div className="flex items-center justify-between gap-3">
                                                    <p className="text-sm font-medium text-hud-text-primary">
                                                        {artist.artist_name}
                                                    </p>
                                                    <span className="text-xs text-hud-text-muted">
                                                        affinity {artist.affinity_score.toFixed(2)}
                                                    </span>
                                                </div>
                                                <p className="mt-2 text-sm leading-6 text-hud-text-secondary">
                                                    {artist.reason}
                                                </p>
                                            </button>
                                        ))}
                                    </div>
                                </div>

                                <div>
                                    <p className="mb-3 text-xs font-semibold uppercase tracking-[0.22em] text-hud-text-muted">
                                        Suggested Genres
                                    </p>
                                    <div className="space-y-2">
                                        {bootstrap.suggested_genres.map((genre) => (
                                            <button
                                                key={genre.genre}
                                                type="button"
                                                onClick={() =>
                                                    updateWorkspace({
                                                        seedGenresText: mergeCsv(
                                                            workspace.seedGenresText,
                                                            genre.genre,
                                                        ),
                                                    })
                                                }
                                                className="w-full rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 px-4 py-3 text-left transition-hud hover:border-hud-border-primary"
                                            >
                                                <div className="flex items-center justify-between gap-3">
                                                    <p className="text-sm font-medium text-hud-text-primary">
                                                        {genre.genre}
                                                    </p>
                                                    <span className="text-xs text-hud-text-muted">
                                                        weight {genre.weight.toFixed(2)}
                                                    </span>
                                                </div>
                                                <p className="mt-2 text-sm leading-6 text-hud-text-secondary">
                                                    {genre.reason}
                                                </p>
                                            </button>
                                        ))}
                                    </div>
                                </div>
                            </div>
                        ) : (
                            <div className="rounded-2xl border border-dashed border-hud-border-secondary bg-hud-bg-primary/60 p-4 text-sm leading-6 text-hud-text-secondary">
                                Waiting for PMS bootstrap data from the API.
                            </div>
                        )}
                    </HudCard>

                    <HudCard title="PMS Notes" subtitle="What this screen is responsible for">
                        <div className="space-y-3 text-sm leading-6 text-hud-text-secondary">
                            <p>
                                PMS is where we capture catalog anchors: playlist context, explicit track seeds, artist
                                affinity, and genre direction.
                            </p>
                            <p>
                                The page now hydrates from a Spring Boot bootstrap endpoint. The next step is replacing
                                that static payload with real playlist and library data.
                            </p>
                        </div>
                    </HudCard>
                </div>
            </section>
        </div>
    )
}

export default PmsPage
