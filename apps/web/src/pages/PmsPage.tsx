import { startTransition, useEffect, useMemo, useState } from 'react'
import { LibraryBig, RefreshCw, Sparkles } from 'lucide-react'
import { Link } from 'react-router-dom'
import Button from '@/components/common/Button'
import HudCard from '@/components/common/HudCard'
import PlaylistFeatureCard from '@/components/music/PlaylistFeatureCard'
import TrackFeatureCard from '@/components/music/TrackFeatureCard'
import { useAuthSession } from '@/contexts/AuthSessionContext'
import { usePlayback } from '@/contexts/PlaybackContext'
import { useRecommendationWorkspace } from '@/contexts/RecommendationWorkspaceContext'
import { ApiError, fetchPmsPlaylistImportBootstrap, fetchPmsWorkspaceBootstrap, importPmsPlaylists } from '@/services/api'
import type { PmsPlaylistImportBootstrapResponse, PmsWorkspaceBootstrapResponse } from '@/types/api'

const splitItems = (value: string) =>
    value
        .split(',')
        .map((item) => item.trim())
        .filter(Boolean)

const mergeCsv = (current: string, nextValue: string) => {
    const merged = [...splitItems(current), nextValue]
    return Array.from(new Set(merged)).join(', ')
}

const openExternal = (url?: string | null) => {
    if (!url) {
        return
    }

    window.open(url, '_blank', 'noopener,noreferrer')
}

const PmsPage = () => {
    const { session, updateSession } = useAuthSession()
    const { playItem } = usePlayback()
    const {
        workspace,
        updateWorkspace,
        resetWorkspace,
        seedTrackCount,
        seedArtistCount,
        seedGenreCount,
    } = useRecommendationWorkspace()
    const [bootstrap, setBootstrap] = useState<PmsWorkspaceBootstrapResponse | null>(null)
    const [importBootstrap, setImportBootstrap] = useState<PmsPlaylistImportBootstrapResponse | null>(null)
    const [selectedExternalPlaylistIds, setSelectedExternalPlaylistIds] = useState<string[]>([])
    const [isLoading, setIsLoading] = useState(true)
    const [isImporting, setIsImporting] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [importMessage, setImportMessage] = useState<string | null>(null)

    const activeUserId = session?.userId

    const hydrateWorkspaceFromBootstrap = (response: PmsWorkspaceBootstrapResponse) => {
        updateWorkspace({
            userId: response.workspace_defaults.user_id,
            playlistId: response.workspace_defaults.playlist_id,
            seedTrackIdsText: response.workspace_defaults.seed_track_ids.join(', '),
            seedArtistNamesText: response.workspace_defaults.seed_artist_names.join(', '),
            seedGenresText: response.workspace_defaults.seed_genres.join(', '),
        })
    }

    useEffect(() => {
        const controller = new AbortController()

        setIsLoading(true)
        setError(null)

        const load = async () => {
            try {
                const [workspaceResponse, importResponse] = await Promise.all([
                    fetchPmsWorkspaceBootstrap(
                        activeUserId,
                        workspace.playlistId || undefined,
                        controller.signal,
                    ),
                    activeUserId
                        ? fetchPmsPlaylistImportBootstrap(activeUserId, controller.signal)
                        : Promise.resolve(null),
                ])

                startTransition(() => {
                    setBootstrap(workspaceResponse)
                    setImportBootstrap(importResponse)
                    setSelectedExternalPlaylistIds((current) => {
                        const nextAvailable = importResponse?.available_playlists
                            .filter((playlist) => !playlist.already_imported)
                            .map((playlist) => playlist.external_playlist_id) ?? []

                        if (current.length > 0) {
                            return current.filter((playlistId) => nextAvailable.includes(playlistId))
                        }

                        return nextAvailable.slice(0, 1)
                    })
                    setError(null)
                })

                hydrateWorkspaceFromBootstrap(workspaceResponse)
                updateWorkspace({
                    userId: activeUserId ?? workspaceResponse.workspace_defaults.user_id,
                    preferredPlatformId: session?.preferredPlatformId ?? workspace.preferredPlatformId,
                })
            } catch (requestError: unknown) {
                if (requestError instanceof DOMException && requestError.name === 'AbortError') {
                    return
                }

                const message =
                    requestError instanceof ApiError
                        ? requestError.message
                        : 'Unable to load PMS media shelves from the Spring Boot API.'

                startTransition(() => {
                    setError(message)
                })
            } finally {
                setIsLoading(false)
            }
        }

        void load()

        return () => controller.abort()
    }, [activeUserId, session?.preferredPlatformId, updateWorkspace, workspace.playlistId, workspace.preferredPlatformId])

    const activePlaylist = useMemo(
        () =>
            bootstrap?.playlists.find((playlist) => playlist.playlist_id === workspace.playlistId) ??
            bootstrap?.playlists[0] ??
            null,
        [bootstrap, workspace.playlistId],
    )

    const importablePlaylists = useMemo(
        () => importBootstrap?.available_playlists.filter((playlist) => !playlist.already_imported) ?? [],
        [importBootstrap],
    )

    const importedPlaylists = importBootstrap?.imported_playlists ?? []
    const reconnectRequired = importBootstrap?.platform_connection.reconnect_required ?? false
    const pmsImportSupported = importBootstrap?.platform_connection.pms_import_supported ?? true

    const togglePlaylistSelection = (externalPlaylistId: string) => {
        setSelectedExternalPlaylistIds((current) =>
            current.includes(externalPlaylistId)
                ? current.filter((playlistId) => playlistId !== externalPlaylistId)
                : [...current, externalPlaylistId],
        )
    }

    const reloadPmsData = async (playlistId?: string) => {
        const [workspaceResponse, importResponse] = await Promise.all([
            fetchPmsWorkspaceBootstrap(activeUserId, playlistId ?? workspace.playlistId ?? undefined),
            activeUserId ? fetchPmsPlaylistImportBootstrap(activeUserId) : Promise.resolve(null),
        ])

        setBootstrap(workspaceResponse)
        setImportBootstrap(importResponse)
        setSelectedExternalPlaylistIds(
            importResponse?.available_playlists
                .filter((playlist) => !playlist.already_imported)
                .map((playlist) => playlist.external_playlist_id)
                .slice(0, 1) ?? [],
        )
        hydrateWorkspaceFromBootstrap(workspaceResponse)
    }

    const handleImportPlaylists = async () => {
        if (!session || !importBootstrap) {
            setError('Create an account and connect a preferred platform before importing playlists.')
            return
        }

        if (selectedExternalPlaylistIds.length === 0) {
            setError('Choose at least one connected platform playlist to import into PMS.')
            return
        }

        setIsImporting(true)
        setError(null)
        setImportMessage(null)

        try {
            const response = await importPmsPlaylists({
                user_id: session.userId,
                platform_id: importBootstrap.platform_connection.platform_id,
                external_playlist_ids: selectedExternalPlaylistIds,
            })

            await reloadPmsData()
            updateSession({
                onboardingStage: 'pms-imported',
                nextStepPath: response.next_step.path,
                nextStepMessage: response.next_step.message,
            })
            setImportMessage(response.next_step.message)
        } catch (requestError: unknown) {
            const message =
                requestError instanceof ApiError
                    ? requestError.message
                    : 'Unable to import the selected platform playlists into PMS.'
            setError(message)
            if (requestError instanceof ApiError && requestError.code === 'platform_reconnect_required') {
                setImportMessage(null)
            }
        } finally {
            setIsImporting(false)
        }
    }

    return (
        <div className="space-y-6">
            <section className="grid gap-6 xl:grid-cols-[1.25fr_0.75fr]">
                <HudCard
                    title="Selected PMS Playlist"
                    subtitle="The active playlist drives the current track shelf, seed defaults, and later EMS analysis"
                    action={
                        isLoading ? (
                            <span className="inline-flex items-center gap-2 text-xs text-hud-text-muted">
                                <RefreshCw size={14} className="animate-spin" />
                                Loading media
                            </span>
                        ) : null
                    }
                >
                    {activePlaylist ? (
                        <PlaylistFeatureCard
                            title={activePlaylist.title}
                            sourcePlatform={activePlaylist.source_platform}
                            curator={activePlaylist.curator}
                            trackCount={activePlaylist.track_count}
                            description={activePlaylist.highlight}
                            imageUrl={activePlaylist.cover_image_url}
                            isActive
                            actionLabel="Current Playlist"
                            onPlay={() =>
                                playItem({
                                    id: `playlist:${activePlaylist.playlist_id}`,
                                    kind: 'playlist',
                                    title: activePlaylist.title,
                                    subtitle: `${activePlaylist.curator} · ${activePlaylist.source_platform}`,
                                    sourcePlatform: activePlaylist.source_platform,
                                    imageUrl: activePlaylist.cover_image_url,
                                    externalUrl: activePlaylist.platform_external_url,
                                    platformUri: activePlaylist.platform_uri,
                                    supportingText: activePlaylist.highlight,
                                })
                            }
                            onOpenExternal={() => openExternal(activePlaylist.platform_external_url)}
                        />
                    ) : (
                        <div className="rounded-[24px] border border-dashed border-hud-border-secondary bg-hud-bg-primary/60 p-6 text-sm leading-6 text-hud-text-secondary">
                            Choose or import a playlist to start the PMS media workspace.
                        </div>
                    )}
                </HudCard>

                <HudCard title="Seed Workspace" subtitle="Editable seeds that flow forward to EMS and GMS">
                    <div className="grid gap-4 sm:grid-cols-3">
                        <div className="rounded-[24px] border border-hud-border-secondary bg-hud-bg-primary/75 p-4">
                            <p className="text-[11px] uppercase tracking-[0.24em] text-hud-text-muted">Track Seeds</p>
                            <p className="mt-2 text-3xl font-semibold text-hud-text-primary">{seedTrackCount}</p>
                        </div>
                        <div className="rounded-[24px] border border-hud-border-secondary bg-hud-bg-primary/75 p-4">
                            <p className="text-[11px] uppercase tracking-[0.24em] text-hud-text-muted">Artist Seeds</p>
                            <p className="mt-2 text-3xl font-semibold text-hud-text-primary">{seedArtistCount}</p>
                        </div>
                        <div className="rounded-[24px] border border-hud-border-secondary bg-hud-bg-primary/75 p-4">
                            <p className="text-[11px] uppercase tracking-[0.24em] text-hud-text-muted">Genre Seeds</p>
                            <p className="mt-2 text-3xl font-semibold text-hud-text-primary">{seedGenreCount}</p>
                        </div>
                    </div>

                    <div className="mt-5 space-y-4">
                        <div>
                            <label className="mb-2 block text-sm font-medium text-hud-text-secondary">Seed Track IDs</label>
                            <textarea
                                value={workspace.seedTrackIdsText}
                                onChange={(event) => updateWorkspace({ seedTrackIdsText: event.target.value })}
                                rows={4}
                                className="w-full rounded-2xl border border-hud-border-secondary bg-hud-bg-primary px-4 py-3 text-sm text-hud-text-primary outline-none transition-hud focus:border-hud-border-primary"
                            />
                        </div>

                        <div>
                            <label className="mb-2 block text-sm font-medium text-hud-text-secondary">Seed Artist Names</label>
                            <textarea
                                value={workspace.seedArtistNamesText}
                                onChange={(event) => updateWorkspace({ seedArtistNamesText: event.target.value })}
                                rows={3}
                                className="w-full rounded-2xl border border-hud-border-secondary bg-hud-bg-primary px-4 py-3 text-sm text-hud-text-primary outline-none transition-hud focus:border-hud-border-primary"
                            />
                        </div>

                        <div>
                            <label className="mb-2 block text-sm font-medium text-hud-text-secondary">Seed Genres</label>
                            <textarea
                                value={workspace.seedGenresText}
                                onChange={(event) => updateWorkspace({ seedGenresText: event.target.value })}
                                rows={3}
                                className="w-full rounded-2xl border border-hud-border-secondary bg-hud-bg-primary px-4 py-3 text-sm text-hud-text-primary outline-none transition-hud focus:border-hud-border-primary"
                            />
                        </div>
                    </div>

                    <div className="mt-6 flex flex-wrap gap-3">
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
            </section>

            <HudCard title="Playlist Shelf" subtitle="Every PMS page now keeps the actual playlist context visible">
                {bootstrap?.playlists.length ? (
                    <div className="grid gap-5 lg:grid-cols-2">
                        {bootstrap.playlists.map((playlist) => (
                            <PlaylistFeatureCard
                                key={playlist.playlist_id}
                                title={playlist.title}
                                sourcePlatform={playlist.source_platform}
                                curator={playlist.curator}
                                trackCount={playlist.track_count}
                                description={playlist.highlight}
                                imageUrl={playlist.cover_image_url}
                                isActive={playlist.playlist_id === workspace.playlistId}
                                onSelect={() => updateWorkspace({ playlistId: playlist.playlist_id })}
                                onPlay={() =>
                                    playItem({
                                        id: `playlist:${playlist.playlist_id}`,
                                        kind: 'playlist',
                                        title: playlist.title,
                                        subtitle: `${playlist.curator} · ${playlist.source_platform}`,
                                        sourcePlatform: playlist.source_platform,
                                        imageUrl: playlist.cover_image_url,
                                        externalUrl: playlist.platform_external_url,
                                        platformUri: playlist.platform_uri,
                                        supportingText: playlist.highlight,
                                    })
                                }
                                onOpenExternal={() => openExternal(playlist.platform_external_url)}
                            />
                        ))}
                    </div>
                ) : (
                    <div className="rounded-[24px] border border-dashed border-hud-border-secondary bg-hud-bg-primary/60 p-6 text-sm leading-6 text-hud-text-secondary">
                        Playlist cards will appear here once PMS bootstrap data is available.
                    </div>
                )}
            </HudCard>

            <HudCard title="Track Shelf" subtitle="Relevant album art, playable tracks, and one-click seed actions">
                {bootstrap?.suggested_tracks.length ? (
                    <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-4">
                        {bootstrap.suggested_tracks.map((track) => (
                            <TrackFeatureCard
                                key={track.track_id}
                                title={track.title}
                                artistName={track.artist_name}
                                sourcePlatform={track.source_platform}
                                albumTitle={track.album_title}
                                imageUrl={track.album_image_url}
                                durationMs={track.duration_ms}
                                badges={[
                                    track.seed ? 'seed' : 'candidate',
                                    track.spotify_audio_features_filled ? 'audio ready' : 'pending audio',
                                ]}
                                reason={`Audio features resolved by ${track.spotify_audio_feature_source}.`}
                                onPlay={() =>
                                    playItem({
                                        id: `track:${track.track_id}`,
                                        kind: 'track',
                                        title: track.title,
                                        subtitle: `${track.artist_name} · ${track.source_platform}`,
                                        sourcePlatform: track.source_platform,
                                        imageUrl: track.album_image_url,
                                        albumTitle: track.album_title,
                                        externalUrl: track.platform_external_url,
                                        platformUri: track.platform_uri,
                                        previewUrl: track.preview_url,
                                        spotifyTrackId: track.spotify_track_id,
                                        durationMs: track.duration_ms,
                                        supportingText: activePlaylist?.title ?? null,
                                    })
                                }
                                onUseAsSeed={() =>
                                    updateWorkspace({
                                        seedTrackIdsText: mergeCsv(workspace.seedTrackIdsText, track.track_id),
                                        seedArtistNamesText: mergeCsv(workspace.seedArtistNamesText, track.artist_name),
                                    })
                                }
                                onOpenExternal={() => openExternal(track.platform_external_url)}
                            />
                        ))}
                    </div>
                ) : (
                    <div className="rounded-[24px] border border-dashed border-hud-border-secondary bg-hud-bg-primary/60 p-6 text-sm leading-6 text-hud-text-secondary">
                        Relevant tracks for the selected PMS playlist will appear here.
                    </div>
                )}
            </HudCard>

            <HudCard
                title="Platform Import Queue"
                subtitle="Connected platform playlists that can be pulled into the PMS library"
                action={
                    isImporting ? (
                        <span className="inline-flex items-center gap-2 text-xs text-hud-text-muted">
                            <RefreshCw size={14} className="animate-spin" />
                            Importing
                        </span>
                    ) : null
                }
            >
                <div className="space-y-5">
                    {!session ? (
                        <div className="rounded-[24px] border border-dashed border-hud-border-secondary bg-hud-bg-primary/60 p-6 text-sm leading-6 text-hud-text-secondary">
                            Create an account and connect a streaming platform first. PMS import attaches these
                            playlists to a specific member profile.
                        </div>
                    ) : (
                        <>
                            <div className="rounded-[24px] border border-hud-border-secondary bg-hud-bg-primary/75 p-5">
                                <p className="text-[11px] uppercase tracking-[0.24em] text-hud-text-muted">Preferred Platform</p>
                                <h3 className="mt-3 text-xl font-semibold text-hud-text-primary">
                                    {importBootstrap?.platform_connection.display_name ?? session.preferredPlatformId}
                                </h3>
                                <p className="mt-2 text-sm leading-6 text-hud-text-secondary">
                                    {importBootstrap?.summary.next_step_message ?? 'Connect a platform to import PMS playlists.'}
                                </p>
                            </div>

                            {importMessage && (
                                <div className="rounded-[24px] border border-hud-accent-primary/40 bg-hud-accent-primary/10 p-4 text-sm leading-6 text-hud-text-secondary">
                                    {importMessage}
                                </div>
                            )}

                            {reconnectRequired && (
                                <div className="rounded-[24px] border border-hud-accent-warning/40 bg-hud-accent-warning/10 p-4 text-sm leading-6 text-hud-text-secondary">
                                    Reconnect the preferred platform first so PMS can keep importing playable library
                                    content.
                                </div>
                            )}

                            {!pmsImportSupported && (
                                <div className="rounded-[24px] border border-hud-accent-info/40 bg-hud-accent-info/10 p-4 text-sm leading-6 text-hud-text-secondary">
                                    This preferred platform is useful for long-term analysis signals, but PMS playlist
                                    import is not ready yet.
                                </div>
                            )}

                            {importablePlaylists.length > 0 && (
                                <div className="grid gap-5 lg:grid-cols-2">
                                    {importablePlaylists.map((playlist) => {
                                        const selected = selectedExternalPlaylistIds.includes(playlist.external_playlist_id)
                                        return (
                                            <PlaylistFeatureCard
                                                key={playlist.external_playlist_id}
                                                title={playlist.title}
                                                sourcePlatform={playlist.source_platform}
                                                curator={playlist.curator}
                                                trackCount={playlist.track_count}
                                                description={playlist.description}
                                                imageUrl={playlist.cover_image_url}
                                                isActive={selected}
                                                actionLabel={selected ? 'Queued for Import' : 'Queue for Import'}
                                                onSelect={() => togglePlaylistSelection(playlist.external_playlist_id)}
                                                onPlay={() =>
                                                    playItem({
                                                        id: `import-playlist:${playlist.external_playlist_id}`,
                                                        kind: 'playlist',
                                                        title: playlist.title,
                                                        subtitle: `${playlist.curator} · ${playlist.source_platform}`,
                                                        sourcePlatform: playlist.source_platform,
                                                        imageUrl: playlist.cover_image_url,
                                                        externalUrl: playlist.platform_external_url,
                                                        platformUri: playlist.platform_uri,
                                                        supportingText: playlist.description,
                                                    })
                                                }
                                                onOpenExternal={() => openExternal(playlist.platform_external_url)}
                                            />
                                        )
                                    })}
                                </div>
                            )}

                            {importedPlaylists.length > 0 && (
                                <div className="space-y-3">
                                    <p className="text-[11px] uppercase tracking-[0.24em] text-hud-text-muted">Already Imported</p>
                                    <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                                        {importedPlaylists.map((playlist) => (
                                            <PlaylistFeatureCard
                                                key={playlist.playlist_id}
                                                title={playlist.title}
                                                sourcePlatform={playlist.source_platform}
                                                curator="pms library"
                                                trackCount={playlist.track_count}
                                                description={`Imported ${new Date(playlist.imported_at).toLocaleString()}`}
                                                imageUrl={playlist.cover_image_url}
                                                onSelect={() => updateWorkspace({ playlistId: playlist.playlist_id })}
                                                onPlay={() =>
                                                    playItem({
                                                        id: `library-playlist:${playlist.playlist_id}`,
                                                        kind: 'playlist',
                                                        title: playlist.title,
                                                        subtitle: `${playlist.source_platform} · PMS library`,
                                                        sourcePlatform: playlist.source_platform,
                                                        imageUrl: playlist.cover_image_url,
                                                        externalUrl: playlist.platform_external_url,
                                                        platformUri: playlist.platform_uri,
                                                        supportingText: `Imported ${new Date(playlist.imported_at).toLocaleString()}`,
                                                    })
                                                }
                                                onOpenExternal={() => openExternal(playlist.platform_external_url)}
                                            />
                                        ))}
                                    </div>
                                </div>
                            )}

                            <div className="flex flex-wrap gap-3">
                                <Button
                                    type="button"
                                    variant="primary"
                                    glow
                                    disabled={isImporting || selectedExternalPlaylistIds.length === 0 || reconnectRequired}
                                    onClick={handleImportPlaylists}
                                >
                                    <LibraryBig size={18} />
                                    Import Selected Playlists
                                </Button>
                                <Link to="/platforms">
                                    <Button type="button" variant="outline">
                                        Manage Platform Connections
                                    </Button>
                                </Link>
                                <Link to="/ems">
                                    <Button type="button" variant="ghost">
                                        <Sparkles size={18} />
                                        Continue to EMS
                                    </Button>
                                </Link>
                            </div>
                        </>
                    )}

                    {error && (
                        <div className="rounded-[24px] border border-hud-accent-danger/40 bg-hud-accent-danger/10 p-4 text-sm leading-6 text-hud-text-secondary">
                            {error}
                        </div>
                    )}
                </div>
            </HudCard>
        </div>
    )
}

export default PmsPage
