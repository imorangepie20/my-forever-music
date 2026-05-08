import { startTransition, useEffect, useMemo, useState, type FormEvent } from 'react'
import { Activity, Bookmark, Heart, RefreshCw, Sparkles, ThumbsDown } from 'lucide-react'
import { Link } from 'react-router-dom'
import Button from '@/components/common/Button'
import HudCard from '@/components/common/HudCard'
import PlaylistFeatureCard from '@/components/music/PlaylistFeatureCard'
import TrackFeatureCard from '@/components/music/TrackFeatureCard'
import { useAuthSession } from '@/contexts/AuthSessionContext'
import { usePlayback } from '@/contexts/PlaybackContext'
import { useRecommendationWorkspace } from '@/contexts/RecommendationWorkspaceContext'
import {
    ApiError,
    fetchPmsWorkspaceBootstrap,
    previewGmsRecommendations,
    recordGmsRecommendationFeedback,
    saveTrackToPmsPersonalPlaylist,
} from '@/services/api'
import type { GmsRecommendationFeedbackType, GmsRecommendationPreviewResponse } from '@/types/api'

const openExternal = (url?: string | null) => {
    if (!url) {
        return
    }

    window.open(url, '_blank', 'noopener,noreferrer')
}

const GmsPreviewPage = () => {
    const { session } = useAuthSession()
    const { workspace, updateWorkspace } = useRecommendationWorkspace()
    const { playItem } = usePlayback()
    const [isSubmitting, setIsSubmitting] = useState(false)
    const [isContextLoading, setIsContextLoading] = useState(true)
    const [contextError, setContextError] = useState<string | null>(null)
    const [response, setResponse] = useState<GmsRecommendationPreviewResponse | null>(null)
    const [bootstrap, setBootstrap] = useState<Awaited<ReturnType<typeof fetchPmsWorkspaceBootstrap>> | null>(null)
    const [feedbackByTrackId, setFeedbackByTrackId] = useState<Record<string, GmsRecommendationFeedbackType>>({})
    const [feedbackPendingTrackId, setFeedbackPendingTrackId] = useState<string | null>(null)
    const [saveMessage, setSaveMessage] = useState<string | null>(null)
    const activeUserId = session?.userId || workspace.userId

    useEffect(() => {
        const controller = new AbortController()
        setIsContextLoading(true)
        setContextError(null)

        fetchPmsWorkspaceBootstrap(
            activeUserId || undefined,
            workspace.playlistId || undefined,
            controller.signal,
        )
            .then((workspaceResponse) => {
                startTransition(() => {
                    setBootstrap(workspaceResponse)
                    setContextError(null)
                })
            })
            .catch((requestError: unknown) => {
                if (requestError instanceof DOMException && requestError.name === 'AbortError') {
                    return
                }

                const message =
                    requestError instanceof ApiError
                        ? requestError.message
                        : 'Unable to load the current PMS playlist context for GMS.'

                startTransition(() => {
                    setContextError(message)
                })
            })
            .finally(() => {
                setIsContextLoading(false)
            })

        return () => controller.abort()
    }, [activeUserId, workspace.playlistId])

    const activePlaylist = useMemo(
        () =>
            bootstrap?.playlists.find((playlist) => playlist.playlist_id === workspace.playlistId) ??
            bootstrap?.playlists[0] ??
            null,
        [bootstrap, workspace.playlistId],
    )

    const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault()
        if (!session || !activePlaylist) {
            setContextError('Import a real PMS playlist before requesting GMS recommendations.')
            return
        }

        setIsSubmitting(true)
        setContextError(null)

        const payload = {
            request_id: `web-preview-${Date.now()}`,
            user_id: session.userId,
            playlist_id: activePlaylist.playlist_id,
            mood: workspace.mood,
            energy_level: workspace.energyLevel,
            familiarity_bias: workspace.familiarityBias,
            limit: workspace.limit,
            include_explanations: workspace.includeExplanations,
        }

        try {
            const preview = await previewGmsRecommendations(payload)
            startTransition(() => {
                setResponse(preview)
                setFeedbackByTrackId({})
                setSaveMessage(null)
                setContextError(null)
            })
        } catch (requestError: unknown) {
            const message =
                requestError instanceof ApiError
                    ? requestError.message
                    : 'Unable to get a playable preview from the Spring Boot bridge.'

            startTransition(() => {
                setContextError(message)
            })
        } finally {
            setIsSubmitting(false)
        }
    }

    const handleFeedback = async (
        item: GmsRecommendationPreviewResponse['items'][number],
        feedbackType: GmsRecommendationFeedbackType,
    ) => {
        if (!session || !response) {
            setContextError('Log in and request a GMS preview before recording recommendation feedback.')
            return
        }

        setFeedbackPendingTrackId(item.track_id)
        setContextError(null)

        try {
            await recordGmsRecommendationFeedback({
                user_id: session.userId,
                request_id: response.request_id,
                playlist_id: item.source_playlist_id ?? activePlaylist?.playlist_id ?? workspace.playlistId,
                track_id: item.track_id,
                feedback_type: feedbackType,
                score: feedbackType === 'like' || feedbackType === 'save' ? 1 : -1,
                source_space: item.source_space,
                reason: item.reason,
            })

            if (feedbackType === 'save') {
                const saveResponse = await saveTrackToPmsPersonalPlaylist({
                    user_id: session.userId,
                    target_playlist_title: 'Saved GMS Recommendations',
                    track_id: item.track_id,
                    source_context: 'gms-preview',
                })
                setSaveMessage(
                    `${item.title} was saved to ${saveResponse.playlist.title}. ${saveResponse.playlist.track_count} tracks are in that playlist.`,
                )
            }

            startTransition(() => {
                setFeedbackByTrackId((current) => ({
                    ...current,
                    [item.track_id]: feedbackType,
                }))
            })
        } catch (requestError: unknown) {
            const message =
                requestError instanceof ApiError
                    ? requestError.message
                    : 'Unable to record this recommendation feedback.'

            startTransition(() => {
                setContextError(message)
            })
        } finally {
            setFeedbackPendingTrackId(null)
        }
    }

    return (
        <div className="space-y-6">
            <section className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
                <div className="space-y-6">
                    <HudCard title="GMS Approval Request" subtitle="Generate candidates from PMS and EMS context for final user approval">
                        <form className="space-y-5" onSubmit={handleSubmit}>
                            <div>
                                <label className="mb-2 block text-sm font-medium text-hud-text-secondary">Mood</label>
                                <select
                                    value={workspace.mood}
                                    onChange={(event) => updateWorkspace({ mood: event.target.value as typeof workspace.mood })}
                                    className="w-full rounded-xl border border-hud-border-secondary bg-hud-bg-primary px-4 py-3 text-sm text-hud-text-primary outline-none transition-hud focus:border-hud-border-primary"
                                >
                                    <option value="focus">Focus</option>
                                    <option value="calm">Calm</option>
                                    <option value="upbeat">Upbeat</option>
                                    <option value="melancholy">Melancholy</option>
                                    <option value="discovery">Discovery</option>
                                </select>
                            </div>

                            <div className="grid grid-cols-3 gap-3">
                                <div>
                                    <label className="mb-2 block text-sm font-medium text-hud-text-secondary">Energy</label>
                                    <input
                                        value={workspace.energyLevel}
                                        onChange={(event) => updateWorkspace({ energyLevel: Number(event.target.value) })}
                                        type="number"
                                        min="1"
                                        max="5"
                                        className="w-full rounded-xl border border-hud-border-secondary bg-hud-bg-primary px-4 py-3 text-sm text-hud-text-primary outline-none transition-hud focus:border-hud-border-primary"
                                    />
                                </div>
                                <div>
                                    <label className="mb-2 block text-sm font-medium text-hud-text-secondary">Bias</label>
                                    <input
                                        value={workspace.familiarityBias}
                                        onChange={(event) => updateWorkspace({ familiarityBias: Number(event.target.value) })}
                                        type="number"
                                        min="1"
                                        max="5"
                                        className="w-full rounded-xl border border-hud-border-secondary bg-hud-bg-primary px-4 py-3 text-sm text-hud-text-primary outline-none transition-hud focus:border-hud-border-primary"
                                    />
                                </div>
                                <div>
                                    <label className="mb-2 block text-sm font-medium text-hud-text-secondary">Limit</label>
                                    <input
                                        value={workspace.limit}
                                        onChange={(event) => updateWorkspace({ limit: Number(event.target.value) })}
                                        type="number"
                                        min="1"
                                        max="20"
                                        className="w-full rounded-xl border border-hud-border-secondary bg-hud-bg-primary px-4 py-3 text-sm text-hud-text-primary outline-none transition-hud focus:border-hud-border-primary"
                                    />
                                </div>
                            </div>

                            <label className="flex items-center gap-3 rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 px-4 py-3 text-sm text-hud-text-secondary">
                                <input
                                    checked={workspace.includeExplanations}
                                    onChange={(event) => updateWorkspace({ includeExplanations: event.target.checked })}
                                    type="checkbox"
                                    className="h-4 w-4 rounded border-hud-border-secondary bg-hud-bg-primary text-hud-accent-primary"
                                />
                                Include explanation strings in the preview response
                            </label>

                            <div className="grid gap-4 sm:grid-cols-3">
                                <div className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                                    <p className="text-[11px] uppercase tracking-[0.24em] text-hud-text-muted">PMS Context</p>
                                    <p className="mt-2 text-sm font-semibold text-hud-text-primary">
                                        {activePlaylist ? 'Ready' : 'Missing'}
                                    </p>
                                </div>
                                <div className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                                    <p className="text-[11px] uppercase tracking-[0.24em] text-hud-text-muted">EMS Signal</p>
                                    <p className="mt-2 text-sm font-semibold capitalize text-hud-text-primary">{workspace.mood}</p>
                                </div>
                                <div className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                                    <p className="text-[11px] uppercase tracking-[0.24em] text-hud-text-muted">Approval Path</p>
                                    <p className="mt-2 text-sm font-semibold text-hud-text-primary">{'GMS -> PMS'}</p>
                                </div>
                            </div>

                            <div className="flex flex-wrap gap-3">
                                <Link to="/ems" className="flex-1 min-w-[140px]">
                                    <Button type="button" variant="outline" fullWidth>
                                        Back to EMS
                                    </Button>
                                </Link>
                                <Button type="submit" variant="primary" glow fullWidth disabled={isSubmitting || !session || !activePlaylist}>
                                    {isSubmitting ? (
                                        <>
                                            <RefreshCw size={18} className="animate-spin" />
                                            Generating Preview
                                        </>
                                    ) : (
                                        <>
                                            <Sparkles size={18} />
                                            Request GMS Preview
                                        </>
                                    )}
                                </Button>
                            </div>
                        </form>
                    </HudCard>

                    <HudCard title="Response Feed" subtitle="Strategy, warnings, and bridge status">
                        {contextError ? (
                            <div className="rounded-2xl border border-hud-accent-danger/40 bg-hud-accent-danger/10 p-4 text-sm leading-6 text-hud-text-secondary">
                                {contextError}
                            </div>
                        ) : response ? (
                            <div className="space-y-4">
                                <div className="grid gap-4 md:grid-cols-2">
                                    <div className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                                        <p className="text-xs uppercase tracking-[0.22em] text-hud-text-muted">Request ID</p>
                                        <p className="mt-2 text-sm font-medium text-hud-text-primary">{response.request_id}</p>
                                    </div>
                                    <div className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                                        <p className="text-xs uppercase tracking-[0.22em] text-hud-text-muted">Strategy</p>
                                        <p className="mt-2 text-sm font-medium text-hud-text-primary">{response.context.strategy}</p>
                                    </div>
                                </div>

                                <div className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                                    <div className="flex flex-wrap items-center gap-3">
                                        <span className="rounded-full border border-hud-border-primary bg-hud-accent-primary/10 px-3 py-1 text-xs font-semibold uppercase tracking-[0.22em] text-hud-accent-primary">
                                            {response.context.mode}
                                        </span>
                                        <span className="rounded-full border border-hud-border-secondary px-3 py-1 text-xs font-medium text-hud-text-secondary">
                                            Mood: {response.context.mood ?? 'none'}
                                        </span>
                                        <span className="rounded-full border border-hud-border-secondary px-3 py-1 text-xs font-medium text-hud-text-secondary">
                                            Energy: {response.context.energy_level}
                                        </span>
                                        <span className="rounded-full border border-hud-border-secondary px-3 py-1 text-xs font-medium text-hud-text-secondary">
                                            Engine: {response.context.engine}
                                        </span>
                                    </div>
                                    <p className="mt-4 text-sm leading-6 text-hud-text-secondary">
                                        Generated at {new Date(response.generated_at).toLocaleString()} with {response.items.length} playable recommendation candidates.
                                    </p>
                                </div>

                                {response.warnings.length > 0 && (
                                    <div className="rounded-2xl border border-hud-accent-warning/40 bg-hud-accent-warning/10 p-4">
                                        <p className="text-xs uppercase tracking-[0.24em] text-hud-accent-warning">Warnings</p>
                                        <ul className="mt-3 space-y-2 text-sm leading-6 text-hud-text-secondary">
                                            {response.warnings.map((warning) => (
                                                <li key={warning}>{warning}</li>
                                            ))}
                                        </ul>
                                    </div>
                                )}

                                {saveMessage && (
                                    <div className="rounded-2xl border border-hud-accent-primary/40 bg-hud-accent-primary/10 p-4 text-sm leading-6 text-hud-text-secondary">
                                        {saveMessage}
                                    </div>
                                )}
                            </div>
                        ) : (
                            <div className="rounded-2xl border border-dashed border-hud-border-secondary bg-hud-bg-primary/60 p-6 text-sm leading-6 text-hud-text-secondary">
                                Submit a preview request to see the bridge response and playable candidate shelf.
                            </div>
                        )}
                    </HudCard>
                </div>

                <div className="space-y-6">
                    <HudCard
                        title="Current PMS Playlist"
                        subtitle="The selected playlist and its imagery stay on-screen while GMS results are generated"
                        action={
                            isContextLoading ? (
                                <span className="inline-flex items-center gap-2 text-xs text-hud-text-muted">
                                    <RefreshCw size={14} className="animate-spin" />
                                    Loading context
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
                                actionLabel="Current GMS Basis"
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
                            <div className="rounded-2xl border border-dashed border-hud-border-secondary bg-hud-bg-primary/60 p-6 text-sm leading-6 text-hud-text-secondary">
                                Pick a PMS playlist first so GMS can rank real playable tracks.
                            </div>
                        )}
                    </HudCard>

                    <HudCard title="Recommendation Candidates" subtitle="Playable tracks resolved back into the PMS user library">
                        {response ? (
                            <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
                                {response.items.map((item) => (
                                    <TrackFeatureCard
                                        key={item.track_id}
                                        title={item.title}
                                        artistName={item.artist_name}
                                        sourcePlatform={item.source_platform}
                                        albumTitle={item.album_title}
                                        imageUrl={item.album_image_url}
                                        durationMs={item.duration_ms}
                                        reason={item.reason}
                                        badges={[
                                            `rank ${item.rank}`,
                                            `score ${item.score.toFixed(2)}`,
                                            item.source_playlist_title ? 'library resolved' : item.source_space,
                                        ]}
                                        onPlay={() =>
                                            playItem({
                                                id: `gms:${item.track_id}`,
                                                kind: 'track',
                                                title: item.title,
                                                subtitle: `${item.artist_name} · ${item.source_platform}`,
                                                sourcePlatform: item.source_platform,
                                                imageUrl: item.album_image_url,
                                                albumTitle: item.album_title,
                                                externalUrl: item.platform_external_url,
                                                platformUri: item.platform_uri,
                                                previewUrl: item.preview_url,
                                                spotifyTrackId: item.audio_feature_track_id ?? item.spotify_track_id,
                                                durationMs: item.duration_ms,
                                                supportingText: item.source_playlist_title ?? activePlaylist?.title ?? null,
                                            })
                                        }
                                        onOpenExternal={() => openExternal(item.platform_external_url)}
                                        feedbackActions={[
                                            {
                                                label: 'Like',
                                                icon: <Heart size={16} />,
                                                active: feedbackByTrackId[item.track_id] === 'like',
                                                disabled: feedbackPendingTrackId === item.track_id,
                                                onClick: () => handleFeedback(item, 'like'),
                                            },
                                            {
                                                label: 'Pass',
                                                icon: <ThumbsDown size={16} />,
                                                active: feedbackByTrackId[item.track_id] === 'dislike',
                                                disabled: feedbackPendingTrackId === item.track_id,
                                                onClick: () => handleFeedback(item, 'dislike'),
                                            },
                                            {
                                                label: 'Save',
                                                icon: <Bookmark size={16} />,
                                                active: feedbackByTrackId[item.track_id] === 'save',
                                                disabled: feedbackPendingTrackId === item.track_id,
                                                onClick: () => handleFeedback(item, 'save'),
                                            },
                                        ]}
                                    />
                                ))}
                            </div>
                        ) : (
                            <div className="flex items-center gap-3 rounded-2xl border border-dashed border-hud-border-secondary bg-hud-bg-primary/60 p-6 text-sm text-hud-text-secondary">
                                <Activity size={18} className="text-hud-accent-primary" />
                                Candidate tracks will appear here once the preview request completes.
                            </div>
                        )}
                    </HudCard>
                </div>
            </section>
        </div>
    )
}

export default GmsPreviewPage
