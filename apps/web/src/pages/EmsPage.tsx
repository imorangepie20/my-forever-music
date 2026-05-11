import { startTransition, useEffect, useMemo, useState, type FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { ListMusic, RefreshCw, Search } from 'lucide-react'
import Button from '@/components/common/Button'
import HudCard from '@/components/common/HudCard'
import PlaylistFeatureCard from '@/components/music/PlaylistFeatureCard'
import TrackFeatureCard from '@/components/music/TrackFeatureCard'
import { useAuthSession } from '@/contexts/AuthSessionContext'
import { usePlayback } from '@/contexts/PlaybackContext'
import { useRecommendationWorkspace } from '@/contexts/RecommendationWorkspaceContext'
import {
    buildEmsPlaylistDetailPath,
    buildEmsSearchPlaylistDetailPath,
    emsSearchPlaylistCacheKey,
    toEmsSearchTrackPlaybackItem,
    toEmsTrackPlaybackItem,
} from '@/lib/emsPlayback'
import {
    ApiError,
    fetchEmsCollectedPlaylistDetail,
    fetchEmsCollectedPlaylists,
    searchEmsCollection,
} from '@/services/api'
import type {
    EmsCollectionPlaylistItem,
    EmsCollectionSearchPlaylistItem,
    EmsCollectionSearchResponse,
} from '@/types/api'

type DiscoveryPlatformId = string

const defaultDiscoveryPlatformIds: DiscoveryPlatformId[] = ['tidal', 'spotify']
const SEARCH_RESULT_PAGE_SIZE = 12
const SEARCH_CACHE_PREFIX = 'ems-search'

const openExternal = (url?: string | null) => {
    if (!url) {
        return
    }
    window.open(url, '_blank', 'noopener,noreferrer')
}

const formatPercent = (value?: number | null) =>
    `${Math.round((value ?? 0) * 100)}%`

const pageValue = (value: string | null) => {
    const parsed = Number(value)
    return Number.isFinite(parsed) && parsed > 0 ? Math.floor(parsed) : 1
}

const searchCacheKey = (userId: string, platformId: string, query: string) =>
    `${SEARCH_CACHE_PREFIX}:${userId}:${platformId}:${query.trim().toLowerCase()}`

const writeSearchPlaylistCache = (playlist: EmsCollectionSearchPlaylistItem) => {
    window.sessionStorage.setItem(
        emsSearchPlaylistCacheKey(playlist.source_platform, playlist.external_playlist_id),
        JSON.stringify(playlist),
    )
}

const pageCountFor = (count: number) =>
    Math.max(1, Math.ceil(count / SEARCH_RESULT_PAGE_SIZE))

const EmsPage = () => {
    const { session } = useAuthSession()
    const { workspace } = useRecommendationWorkspace()
    const { playItem, playQueue } = usePlayback()
    const navigate = useNavigate()
    const [searchParams, setSearchParams] = useSearchParams()
    const activeUserId = session?.userId || workspace.userId
    const activeSearchPlatformId = session?.preferredPlatformId ?? workspace.preferredPlatformId
    const urlQuery = searchParams.get('q')?.trim() ?? ''
    const playlistPage = pageValue(searchParams.get('playlist_page'))
    const trackPage = pageValue(searchParams.get('track_page'))
    const [searchQuery, setSearchQuery] = useState(urlQuery)
    const [searchResult, setSearchResult] = useState<EmsCollectionSearchResponse | null>(null)
    const [isSearching, setIsSearching] = useState(false)
    const [searchError, setSearchError] = useState<string | null>(null)
    const [publicPlaylistsByPlatform, setPublicPlaylistsByPlatform] =
        useState<Record<DiscoveryPlatformId, EmsCollectionPlaylistItem[]>>({})
    const [isLoadingCollection, setIsLoadingCollection] = useState(false)
    const [preparingPlaylistId, setPreparingPlaylistId] = useState<number | null>(null)
    const [collectionError, setCollectionError] = useState<string | null>(null)

    useEffect(() => {
        const controller = new AbortController()

        setIsLoadingCollection(true)
        setCollectionError(null)

        Promise.all(
            defaultDiscoveryPlatformIds.map(async (providerId) => {
                const response = await fetchEmsCollectedPlaylists(providerId, controller.signal, 12)
                return [providerId, response.playlists] as const
            }),
        )
            .then((entries) => {
                startTransition(() => {
                    setPublicPlaylistsByPlatform(Object.fromEntries(entries))
                })
            })
            .catch((err: unknown) => {
                if (err instanceof DOMException && err.name === 'AbortError') return
                const message =
                    err instanceof ApiError
                        ? err.message
                        : 'Unable to load public playlist pool.'
                startTransition(() => setCollectionError(message))
            })
            .finally(() => {
                setIsLoadingCollection(false)
            })

        return () => controller.abort()
    }, [])

    useEffect(() => {
        setSearchQuery(urlQuery)
        if (!urlQuery) {
            setSearchResult(null)
            setSearchError(null)
            return
        }
        if (!activeUserId) {
            setSearchError('Sign in before searching provider results.')
            return
        }

        const cacheKey = searchCacheKey(activeUserId, activeSearchPlatformId, urlQuery)
        const cached = window.sessionStorage.getItem(cacheKey)
        if (cached) {
            try {
                setSearchResult(JSON.parse(cached) as EmsCollectionSearchResponse)
                setSearchError(null)
                return
            } catch {
                window.sessionStorage.removeItem(cacheKey)
            }
        }

        let isCurrent = true
        setIsSearching(true)
        setSearchError(null)
        searchEmsCollection({
            user_id: activeUserId,
            query: urlQuery,
        })
            .then((response) => {
                if (!isCurrent) {
                    return
                }
                window.sessionStorage.setItem(cacheKey, JSON.stringify(response))
                startTransition(() => setSearchResult(response))
            })
            .catch((requestError: unknown) => {
                if (!isCurrent) {
                    return
                }
                const message =
                    requestError instanceof ApiError
                        ? requestError.message
                        : 'Unable to search EMS provider results.'
                setSearchResult(null)
                setSearchError(message)
            })
            .finally(() => {
                if (isCurrent) {
                    setIsSearching(false)
                }
            })

        return () => {
            isCurrent = false
        }
    }, [activeSearchPlatformId, activeUserId, urlQuery])

    const publicPoolPlaylists = defaultDiscoveryPlatformIds.flatMap((providerId) => publicPlaylistsByPlatform[providerId] ?? [])
    const playlistPageCount = pageCountFor(searchResult?.playlists.length ?? 0)
    const trackPageCount = pageCountFor(searchResult?.tracks.length ?? 0)
    const safePlaylistPage = Math.min(playlistPage, playlistPageCount)
    const safeTrackPage = Math.min(trackPage, trackPageCount)
    const pagedSearchPlaylists = useMemo(
        () => searchResult?.playlists.slice((safePlaylistPage - 1) * SEARCH_RESULT_PAGE_SIZE, safePlaylistPage * SEARCH_RESULT_PAGE_SIZE) ?? [],
        [safePlaylistPage, searchResult],
    )
    const pagedSearchTracks = useMemo(
        () => searchResult?.tracks.slice((safeTrackPage - 1) * SEARCH_RESULT_PAGE_SIZE, safeTrackPage * SEARCH_RESULT_PAGE_SIZE) ?? [],
        [safeTrackPage, searchResult],
    )

    const updateSearchPage = (key: 'playlist_page' | 'track_page', page: number) => {
        const nextParams = new URLSearchParams(searchParams)
        nextParams.set(key, String(page))
        setSearchParams(nextParams)
    }

    const handleSearchSubmit = (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault()
        const trimmedQuery = searchQuery.trim()
        if (!trimmedQuery) {
            return
        }
        setSearchParams({
            q: trimmedQuery,
            playlist_page: '1',
            track_page: '1',
        })
    }

    const handlePlayEmsPlaylist = async (playlist: EmsCollectionPlaylistItem) => {
        setCollectionError(null)
        setPreparingPlaylistId(playlist.id)

        try {
            const detail = await fetchEmsCollectedPlaylistDetail(playlist.id)
            const playbackItems = detail.tracks.map((track) => toEmsTrackPlaybackItem(track, detail.playlist.title))
            if (playbackItems.length === 0) {
                setCollectionError('EMS playlist has no stored tracks to play.')
                return
            }

            await playQueue(playbackItems, 0)
        } catch (requestError: unknown) {
            const message =
                requestError instanceof ApiError
                    ? requestError.message
                    : 'Unable to load EMS playlist tracks for playback.'
            setCollectionError(message)
        } finally {
            setPreparingPlaylistId(null)
        }
    }

    const openSearchPlaylistDetail = (playlist: EmsCollectionSearchPlaylistItem) => {
        writeSearchPlaylistCache(playlist)
        navigate(buildEmsSearchPlaylistDetailPath(playlist.source_platform, playlist.external_playlist_id))
    }

    return (
        <div className="space-y-6">
            <HudCard
                title="EMS Search"
                subtitle="Search connected providers, inspect playlist tracks, and play from the result page"
                action={
                    isSearching ? (
                        <span className="inline-flex items-center gap-2 text-xs text-hud-text-muted">
                            <RefreshCw size={14} className="animate-spin" />
                            Searching
                        </span>
                    ) : null
                }
            >
                <div className="space-y-5">
                    <form className="grid gap-3 lg:grid-cols-[minmax(0,1fr)_auto]" onSubmit={handleSearchSubmit}>
                        <label className="sr-only" htmlFor="ems-search-query">Search query</label>
                        <input
                            id="ems-search-query"
                            value={searchQuery}
                            onChange={(event) => setSearchQuery(event.target.value)}
                            placeholder="Search playlists or tracks"
                            className="h-12 rounded-2xl border border-hud-border-secondary bg-hud-bg-primary px-4 text-sm text-hud-text-primary outline-none transition-hud placeholder:text-hud-text-muted focus:border-hud-border-primary"
                        />
                        <Button type="submit" variant="primary" glow disabled={isSearching || !searchQuery.trim()}>
                            {isSearching ? <RefreshCw size={18} className="animate-spin" /> : <Search size={18} />}
                            Search
                        </Button>
                    </form>

                    {searchError && (
                        <div className="rounded-2xl border border-hud-accent-warning/40 bg-hud-accent-warning/10 p-4 text-sm leading-6 text-hud-text-secondary">
                            {searchError}
                        </div>
                    )}

                    {searchResult && (
                        <div className="space-y-6">
                            <div className="flex flex-wrap gap-3">
                                <span className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 px-4 py-3 text-sm font-medium text-hud-text-primary">
                                    {searchResult.result_playlist_count} playlists
                                </span>
                                <span className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 px-4 py-3 text-sm font-medium text-hud-text-primary">
                                    {searchResult.result_track_count} tracks
                                </span>
                            </div>

                            {searchResult.playlists.length > 0 && (
                                <section className="space-y-3">
                                    <div className="flex flex-wrap items-center justify-between gap-3">
                                        <div className="flex items-center gap-2 text-xs uppercase tracking-[0.22em] text-hud-text-muted">
                                            <ListMusic size={15} />
                                            Playlists
                                        </div>
                                        <ResultPager
                                            page={safePlaylistPage}
                                            pageCount={playlistPageCount}
                                            onPrevious={() => updateSearchPage('playlist_page', Math.max(1, safePlaylistPage - 1))}
                                            onNext={() => updateSearchPage('playlist_page', Math.min(playlistPageCount, safePlaylistPage + 1))}
                                        />
                                    </div>
                                    <div className="grid gap-4 xl:grid-cols-2">
                                        {pagedSearchPlaylists.map((playlist) => (
                                            <PlaylistFeatureCard
                                                key={`${playlist.source_platform}-${playlist.external_playlist_id}`}
                                                title={playlist.title}
                                                sourcePlatform={playlist.source_platform}
                                                curator={playlist.curator || playlist.source_platform}
                                                trackCount={playlist.track_count}
                                                description={playlist.description || 'No description available.'}
                                                imageUrl={playlist.cover_image_url}
                                                actionLabel="Open Tracks"
                                                detailPath={buildEmsSearchPlaylistDetailPath(playlist.source_platform, playlist.external_playlist_id)}
                                                onOpenDetail={() => writeSearchPlaylistCache(playlist)}
                                                onSelect={() => openSearchPlaylistDetail(playlist)}
                                                onOpenExternal={() => openExternal(playlist.platform_external_url)}
                                            />
                                        ))}
                                    </div>
                                </section>
                            )}

                            {searchResult.tracks.length > 0 && (
                                <section className="space-y-3">
                                    <div className="flex flex-wrap items-center justify-between gap-3">
                                        <div className="flex items-center gap-2 text-xs uppercase tracking-[0.22em] text-hud-text-muted">
                                            <Search size={15} />
                                            Tracks
                                        </div>
                                        <ResultPager
                                            page={safeTrackPage}
                                            pageCount={trackPageCount}
                                            onPrevious={() => updateSearchPage('track_page', Math.max(1, safeTrackPage - 1))}
                                            onNext={() => updateSearchPage('track_page', Math.min(trackPageCount, safeTrackPage + 1))}
                                        />
                                    </div>
                                    <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
                                        {pagedSearchTracks.map((track) => (
                                            <TrackFeatureCard
                                                key={`${track.source_platform}-${track.external_track_id}`}
                                                title={track.title}
                                                artistName={track.artist_name}
                                                sourcePlatform={track.source_platform}
                                                albumTitle={track.album_title}
                                                imageUrl={track.album_image_url}
                                                durationMs={track.duration_ms}
                                                badges={track.isrc ? ['ISRC'] : []}
                                                onPlay={() => void playItem(toEmsSearchTrackPlaybackItem(track, 'EMS Search'))}
                                                onOpenExternal={() => openExternal(track.platform_external_url)}
                                            />
                                        ))}
                                    </div>
                                </section>
                            )}

                            {searchResult.playlists.length === 0 && searchResult.tracks.length === 0 && (
                                <div className="rounded-2xl border border-dashed border-hud-border-secondary bg-hud-bg-primary/60 p-6 text-sm leading-6 text-hud-text-secondary">
                                    No provider results found.
                                </div>
                            )}
                        </div>
                    )}
                </div>
            </HudCard>

            <HudCard
                title="EMS Public Playlist Pool"
                subtitle="Stored public playlists rotated from the EMS database"
                action={
                    isLoadingCollection ? (
                        <span className="inline-flex items-center gap-2 text-xs text-hud-text-muted">
                            <RefreshCw size={14} className="animate-spin" />
                            Loading pool
                        </span>
                    ) : null
                }
            >
                <div className="space-y-5">
                    {collectionError && (
                        <div className="rounded-2xl border border-hud-accent-warning/40 bg-hud-accent-warning/10 p-4 text-sm leading-6 text-hud-text-secondary">
                            {collectionError}
                        </div>
                    )}

                    {publicPoolPlaylists.length > 0 ? (
                        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                            {publicPoolPlaylists.map((playlist) => (
                                <PlaylistFeatureCard
                                    key={playlist.id}
                                    title={playlist.title}
                                    sourcePlatform={playlist.source_platform}
                                    curator={playlist.curator}
                                    trackCount={playlist.track_count}
                                    description={playlist.description}
                                    supportingText={`${playlist.audio_feature_coverage.filled_track_count}/${playlist.audio_feature_coverage.track_count} audio features · ${formatPercent(playlist.audio_feature_coverage.coverage_ratio)}`}
                                    imageUrl={playlist.cover_image_url}
                                    actionLabel="Pool Candidate"
                                    detailPath={buildEmsPlaylistDetailPath(playlist.id)}
                                    isPlayLoading={preparingPlaylistId === playlist.id}
                                    onPlay={() => void handlePlayEmsPlaylist(playlist)}
                                    onOpenExternal={() => openExternal(playlist.platform_external_url)}
                                />
                            ))}
                        </div>
                    ) : (
                        <div className="rounded-2xl border border-dashed border-hud-border-secondary bg-hud-bg-primary/60 p-6 text-sm leading-6 text-hud-text-secondary">
                            EMS public playlists will appear here after the scheduled collector stores provider results.
                        </div>
                    )}
                </div>
            </HudCard>
        </div>
    )
}

const ResultPager = ({
    page,
    pageCount,
    onPrevious,
    onNext,
}: {
    page: number
    pageCount: number
    onPrevious: () => void
    onNext: () => void
}) => (
    <div className="flex items-center gap-2">
        <Button type="button" variant="ghost" size="sm" onClick={onPrevious} disabled={page <= 1}>
            Prev
        </Button>
        <span className="min-w-20 text-center text-xs uppercase tracking-[0.18em] text-hud-text-muted">
            {page}/{pageCount}
        </span>
        <Button type="button" variant="ghost" size="sm" onClick={onNext} disabled={page >= pageCount}>
            Next
        </Button>
    </div>
)

export default EmsPage
