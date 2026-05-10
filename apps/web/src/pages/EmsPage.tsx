import { startTransition, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { Activity, AlertTriangle, Brain, Database, RefreshCw, Route, Sparkles } from 'lucide-react'
import { Link } from 'react-router-dom'
import Button from '@/components/common/Button'
import HudCard from '@/components/common/HudCard'
import PlaylistFeatureCard from '@/components/music/PlaylistFeatureCard'
import { usePlayback } from '@/contexts/PlaybackContext'
import { useRecommendationWorkspace } from '@/contexts/RecommendationWorkspaceContext'
import { buildEmsPlaylistDetailPath, toEmsTrackPlaybackItem } from '@/lib/emsPlayback'
import {
    ApiError,
    fetchEmsCollectedPlaylistDetail,
    fetchEmsCollectedPlaylists,
    fetchEmsOverview,
} from '@/services/api'
import type { EmsCollectionPlaylistItem, EmsOverviewResponse } from '@/types/api'

type DiscoveryPlatformId = string

const defaultDiscoveryPlatformIds: DiscoveryPlatformId[] = ['tidal', 'spotify']

const openExternal = (url?: string | null) => {
    if (!url) {
        return
    }
    window.open(url, '_blank', 'noopener,noreferrer')
}

const formatStatus = (status?: string | null) =>
    status ? status.replace(/_/g, ' ') : 'unknown'

const formatDateTime = (value?: string | null) => {
    if (!value) {
        return 'not collected'
    }
    return new Intl.DateTimeFormat('ko-KR', {
        dateStyle: 'short',
        timeStyle: 'short',
    }).format(new Date(value))
}

const formatPercent = (value?: number | null) =>
    `${Math.round((value ?? 0) * 100)}%`

const StatusTile = ({
    label,
    value,
    icon,
}: {
    label: string
    value: string
    icon: ReactNode
}) => (
    <div className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
        <div className="flex items-center gap-3">
            <span className="rounded-xl bg-hud-accent-primary/10 p-2 text-hud-accent-primary">{icon}</span>
            <div>
                <p className="text-xs uppercase tracking-[0.22em] text-hud-text-muted">{label}</p>
                <p className="mt-1 text-lg font-semibold capitalize text-hud-text-primary">{formatStatus(value)}</p>
            </div>
        </div>
    </div>
)

const EmsPage = () => {
    const { workspace } = useRecommendationWorkspace()
    const { playQueue } = usePlayback()
    const [overview, setOverview] = useState<EmsOverviewResponse | null>(null)
    const [isLoadingOverview, setIsLoadingOverview] = useState(true)
    const [overviewError, setOverviewError] = useState<string | null>(null)
    const [publicPlaylistsByPlatform, setPublicPlaylistsByPlatform] =
        useState<Record<DiscoveryPlatformId, EmsCollectionPlaylistItem[]>>({})
    const [isLoadingCollection, setIsLoadingCollection] = useState(false)
    const [preparingPlaylistId, setPreparingPlaylistId] = useState<number | null>(null)
    const [collectionError, setCollectionError] = useState<string | null>(null)

    const discoveryPlatformIds = useMemo(() => {
        const platformIds = new Set(defaultDiscoveryPlatformIds)
        overview?.ems_pool.providers
            .filter((provider) => provider.playlist_count > 0 || provider.track_count > 0)
            .forEach((provider) => platformIds.add(provider.platform_id))
        return Array.from(platformIds)
    }, [overview])

    useEffect(() => {
        const controller = new AbortController()

        setIsLoadingOverview(true)
        setOverviewError(null)

        fetchEmsOverview(
            {
                user_id: workspace.userId || undefined,
                playlist_id: workspace.playlistId || undefined,
            },
            controller.signal,
        )
            .then((response) => {
                startTransition(() => {
                    setOverview(response)
                    setOverviewError(null)
                })
            })
            .catch((requestError: unknown) => {
                if (requestError instanceof DOMException && requestError.name === 'AbortError') {
                    return
                }
                const message =
                    requestError instanceof ApiError
                        ? requestError.message
                        : 'Unable to load the EMS overview.'
                startTransition(() => {
                    setOverview(null)
                    setOverviewError(message)
                })
            })
            .finally(() => {
                setIsLoadingOverview(false)
            })

        return () => controller.abort()
    }, [workspace.playlistId, workspace.userId])

    useEffect(() => {
        const controller = new AbortController()

        setIsLoadingCollection(true)
        setCollectionError(null)

        Promise.all(
            discoveryPlatformIds.map(async (providerId) => {
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
    }, [discoveryPlatformIds])

    const publicPoolPlaylists = discoveryPlatformIds.flatMap((providerId) => publicPlaylistsByPlatform[providerId] ?? [])
    const attentionItems = overview ? [...overview.system_attention, ...overview.warnings] : []

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

    return (
        <div className="space-y-6">
            <HudCard
                title="EMS Overview"
                subtitle="LLM interpretation, deterministic direction, and stored pool health"
                action={
                    isLoadingOverview ? (
                        <span className="inline-flex items-center gap-2 text-xs text-hud-text-muted">
                            <RefreshCw size={14} className="animate-spin" />
                            Loading overview
                        </span>
                    ) : null
                }
            >
                {overviewError ? (
                    <div className="rounded-2xl border border-hud-accent-danger/40 bg-hud-accent-danger/10 p-4 text-sm leading-6 text-hud-text-secondary">
                        {overviewError}
                    </div>
                ) : overview ? (
                    <div className="space-y-6">
                        <div className="grid gap-4 md:grid-cols-3">
                            <StatusTile label="PMS Library" value={overview.pipeline_status.pms_library} icon={<Database size={18} />} />
                            <StatusTile label="EMS Pool" value={overview.pipeline_status.ems_pool} icon={<Activity size={18} />} />
                            <StatusTile label="GMS Readiness" value={overview.pipeline_status.gms_readiness} icon={<Route size={18} />} />
                        </div>

                        <section className="grid gap-5 xl:grid-cols-[1.05fr_0.95fr]">
                            <div className="space-y-4">
                                <div className="flex items-start gap-3">
                                    <span className="rounded-xl bg-hud-accent-primary/10 p-2 text-hud-accent-primary">
                                        <Brain size={18} />
                                    </span>
                                    <div>
                                        <p className="text-xs uppercase tracking-[0.22em] text-hud-text-muted">
                                            {overview.taste_model_snapshot.model ?? formatStatus(overview.taste_model_snapshot.status)}
                                        </p>
                                        <p className="mt-2 text-sm leading-6 text-hud-text-primary">
                                            {overview.taste_model_snapshot.summary ??
                                                'LLM interpretation is waiting for a configured EMS overview model.'}
                                        </p>
                                    </div>
                                </div>
                                <div className="grid gap-3 md:grid-cols-3">
                                    <StatusTile label="Playlists" value={String(overview.pms_context.playlist_count)} icon={<Database size={18} />} />
                                    <StatusTile label="Tracks" value={String(overview.pms_context.library_track_count)} icon={<Activity size={18} />} />
                                    <StatusTile label="Confidence" value={String(overview.taste_model_snapshot.confidence ?? 'pending')} icon={<Sparkles size={18} />} />
                                </div>
                            </div>

                            <div className="space-y-4">
                                <p className="text-sm leading-6 text-hud-text-primary">
                                    {overview.candidate_direction.summary ??
                                        'EMS has deterministic direction values, but no LLM summary has been generated yet.'}
                                </p>
                                <div className="grid gap-3 md:grid-cols-3 xl:grid-cols-1">
                                    <StatusTile label="Mood" value={overview.candidate_direction.mood ?? 'pending'} icon={<Sparkles size={18} />} />
                                    <StatusTile label="Energy" value={String(overview.candidate_direction.energy_level ?? 'pending')} icon={<Activity size={18} />} />
                                    <StatusTile label="Familiarity" value={String(overview.candidate_direction.familiarity_bias ?? 'pending')} icon={<Brain size={18} />} />
                                </div>
                            </div>
                        </section>

                        <section className="grid gap-5 xl:grid-cols-[0.85fr_1.15fr]">
                            <div>
                                <p className="mb-3 text-xs uppercase tracking-[0.22em] text-hud-text-muted">Stored Pool Health</p>
                                <div className="space-y-3">
                                    {overview.ems_pool.providers.map((provider) => (
                                        <div
                                            key={provider.platform_id}
                                            className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4"
                                        >
                                            <div className="flex flex-wrap items-center justify-between gap-3">
                                                <p className="text-sm font-semibold uppercase tracking-[0.16em] text-hud-text-primary">
                                                    {provider.platform_id}
                                                </p>
                                                <span className="rounded-full border border-hud-border-secondary px-3 py-1 text-xs text-hud-text-muted">
                                                    {formatDateTime(provider.last_collected_at)}
                                                </span>
                                            </div>
                                            <p className="mt-2 text-sm text-hud-text-secondary">
                                                {provider.playlist_count} playlists · {provider.track_count} tracks
                                            </p>
                                            <p className="mt-2 text-xs uppercase tracking-[0.18em] text-hud-accent-primary">
                                                {provider.audio_feature_filled_track_count}/{provider.track_count} audio features · {formatPercent(provider.audio_feature_coverage_ratio)}
                                            </p>
                                        </div>
                                    ))}
                                </div>
                            </div>

                            <div className="space-y-4">
                                <div>
                                    <p className="mb-3 text-xs uppercase tracking-[0.22em] text-hud-text-muted">Attention</p>
                                    {attentionItems.length > 0 ? (
                                        <div className="space-y-3">
                                            {attentionItems.slice(0, 3).map((item) => (
                                                <div
                                                    key={item}
                                                    className="flex gap-3 rounded-2xl border border-hud-accent-warning/40 bg-hud-accent-warning/10 p-4 text-sm leading-6 text-hud-text-secondary"
                                                >
                                                    <AlertTriangle size={18} className="mt-0.5 shrink-0 text-hud-accent-warning" />
                                                    <span>{item}</span>
                                                </div>
                                            ))}
                                        </div>
                                    ) : (
                                        <p className="text-sm leading-6 text-hud-text-secondary">
                                            No EMS boundary requires attention right now.
                                        </p>
                                    )}
                                </div>

                                {overview.evidence.length > 0 && (
                                    <div>
                                        <p className="mb-3 text-xs uppercase tracking-[0.22em] text-hud-text-muted">Evidence</p>
                                        <div className="grid gap-3 md:grid-cols-2">
                                            {overview.evidence.slice(0, 4).map((item) => (
                                                <div
                                                    key={item}
                                                    className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4 text-sm leading-6 text-hud-text-secondary"
                                                >
                                                    {item}
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                )}
                            </div>
                        </section>

                        <div className="flex flex-wrap gap-3">
                            <Link to="/gms-preview">
                                <Button type="button" variant="primary" glow>
                                    Review GMS Candidates
                                </Button>
                            </Link>
                            <Link to="/pms">
                                <Button type="button" variant="outline">
                                    Back to PMS
                                </Button>
                            </Link>
                        </div>
                    </div>
                ) : (
                    <div className="text-sm leading-6 text-hud-text-secondary">
                        EMS overview will appear after the workspace context is available.
                    </div>
                )}
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

export default EmsPage
