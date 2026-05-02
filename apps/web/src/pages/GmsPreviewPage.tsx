import { startTransition, useState, type FormEvent } from 'react'
import { Activity, RefreshCw, Sparkles } from 'lucide-react'
import { Link } from 'react-router-dom'
import Button from '@/components/common/Button'
import HudCard from '@/components/common/HudCard'
import { useRecommendationWorkspace } from '@/contexts/RecommendationWorkspaceContext'
import { ApiError, previewGmsRecommendations } from '@/services/api'
import type {
    GmsRecommendationPreviewResponse,
} from '@/types/api'

const splitField = (value: string) =>
    value
        .split(',')
        .map((item) => item.trim())
        .filter(Boolean)

const GmsPreviewPage = () => {
    const {
        workspace,
        updateWorkspace,
        seedTrackCount,
        seedArtistCount,
        seedGenreCount,
    } = useRecommendationWorkspace()
    const [isSubmitting, setIsSubmitting] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [response, setResponse] = useState<GmsRecommendationPreviewResponse | null>(null)

    const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault()
        setIsSubmitting(true)
        setError(null)

        const payload = {
            request_id: `web-preview-${Date.now()}`,
            user_id: workspace.userId || undefined,
            playlist_id: workspace.playlistId || undefined,
            mood: workspace.mood,
            energy_level: workspace.energyLevel,
            familiarity_bias: workspace.familiarityBias,
            limit: workspace.limit,
            seed_track_ids: splitField(workspace.seedTrackIdsText),
            seed_artist_names: splitField(workspace.seedArtistNamesText),
            seed_genres: splitField(workspace.seedGenresText),
            include_explanations: workspace.includeExplanations,
        }

        try {
            const preview = await previewGmsRecommendations(payload)
            startTransition(() => {
                setResponse(preview)
                setError(null)
            })
        } catch (requestError: unknown) {
            const message =
                requestError instanceof ApiError
                    ? requestError.message
                    : 'Unable to get a preview from the Spring Boot bridge.'

            startTransition(() => {
                setError(message)
            })
        } finally {
            setIsSubmitting(false)
        }
    }

    return (
        <div className="grid gap-6 xl:grid-cols-[420px_minmax(0,1fr)]">
            <HudCard title="Preview Request" subtitle="Compose a GMS payload for the Spring Boot bridge">
                <form className="space-y-5" onSubmit={handleSubmit}>
                    <div className="grid gap-3 md:grid-cols-2">
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

                    <div>
                        <label className="mb-2 block text-sm font-medium text-hud-text-secondary">Mood</label>
                        <select
                            value={workspace.mood}
                            onChange={(event) =>
                                updateWorkspace({
                                    mood: event.target.value as typeof workspace.mood,
                                })
                            }
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
                                onChange={(event) =>
                                    updateWorkspace({ energyLevel: Number(event.target.value) })
                                }
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
                                onChange={(event) =>
                                    updateWorkspace({ familiarityBias: Number(event.target.value) })
                                }
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

                    <div>
                        <label className="mb-2 block text-sm font-medium text-hud-text-secondary">Seed Track IDs</label>
                        <textarea
                            value={workspace.seedTrackIdsText}
                            onChange={(event) => updateWorkspace({ seedTrackIdsText: event.target.value })}
                            rows={3}
                            className="w-full rounded-xl border border-hud-border-secondary bg-hud-bg-primary px-4 py-3 text-sm text-hud-text-primary outline-none transition-hud focus:border-hud-border-primary"
                        />
                    </div>

                    <div>
                        <label className="mb-2 block text-sm font-medium text-hud-text-secondary">Seed Artist Names</label>
                        <textarea
                            value={workspace.seedArtistNamesText}
                            onChange={(event) =>
                                updateWorkspace({ seedArtistNamesText: event.target.value })
                            }
                            rows={2}
                            className="w-full rounded-xl border border-hud-border-secondary bg-hud-bg-primary px-4 py-3 text-sm text-hud-text-primary outline-none transition-hud focus:border-hud-border-primary"
                        />
                    </div>

                    <div>
                        <label className="mb-2 block text-sm font-medium text-hud-text-secondary">Seed Genres</label>
                        <textarea
                            value={workspace.seedGenresText}
                            onChange={(event) => updateWorkspace({ seedGenresText: event.target.value })}
                            rows={2}
                            className="w-full rounded-xl border border-hud-border-secondary bg-hud-bg-primary px-4 py-3 text-sm text-hud-text-primary outline-none transition-hud focus:border-hud-border-primary"
                        />
                    </div>

                    <label className="flex items-center gap-3 rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 px-4 py-3 text-sm text-hud-text-secondary">
                        <input
                            checked={workspace.includeExplanations}
                            onChange={(event) =>
                                updateWorkspace({ includeExplanations: event.target.checked })
                            }
                            type="checkbox"
                            className="h-4 w-4 rounded border-hud-border-secondary bg-hud-bg-primary text-hud-accent-primary"
                        />
                        Include explanation strings in the preview response
                    </label>

                    <div className="flex flex-wrap gap-3">
                        <Link to="/ems" className="flex-1 min-w-[140px]">
                            <Button type="button" variant="outline" fullWidth>
                                Back to EMS
                            </Button>
                        </Link>
                        <Button type="submit" variant="primary" glow fullWidth disabled={isSubmitting}>
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

            <div className="space-y-6">
                <HudCard title="Response Feed" subtitle="What the API bridge returned">
                    {error ? (
                        <div className="rounded-2xl border border-hud-accent-danger/40 bg-hud-accent-danger/10 p-4 text-sm leading-6 text-hud-text-secondary">
                            {error}
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
                                    Generated at {new Date(response.generated_at).toLocaleString()} with{' '}
                                    {response.items.length} recommendation candidates.
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
                        </div>
                    ) : (
                        <div className="rounded-2xl border border-dashed border-hud-border-secondary bg-hud-bg-primary/60 p-6 text-sm leading-6 text-hud-text-secondary">
                            Submit a preview request to see the Spring Boot bridge response, context strategy, warnings,
                            and candidate tracks here.
                        </div>
                    )}
                </HudCard>

                <HudCard title="Workspace Snapshot" subtitle="Current PMS and EMS inputs">
                    <div className="grid gap-4 md:grid-cols-2">
                        <div className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                            <p className="text-xs uppercase tracking-[0.22em] text-hud-text-muted">PMS Seeds</p>
                            <p className="mt-2 text-sm leading-6 text-hud-text-primary">
                                Tracks {seedTrackCount}, artists {seedArtistCount}, genres {seedGenreCount}
                            </p>
                            <p className="mt-3 text-xs leading-6 text-hud-text-secondary">
                                Playlist {workspace.playlistId || 'none'} for user {workspace.userId || 'none'}
                            </p>
                        </div>
                        <div className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                            <p className="text-xs uppercase tracking-[0.22em] text-hud-text-muted">EMS Signals</p>
                            <p className="mt-2 text-sm leading-6 text-hud-text-primary">
                                Mood {workspace.mood}, energy {workspace.energyLevel}, familiarity{' '}
                                {workspace.familiarityBias}
                            </p>
                            <p className="mt-3 text-xs leading-6 text-hud-text-secondary">
                                Limit {workspace.limit}, explanations{' '}
                                {workspace.includeExplanations ? 'enabled' : 'disabled'}
                            </p>
                        </div>
                    </div>
                </HudCard>

                <HudCard title="Recommendation Candidates" subtitle="Preview tracks returned by the AI service">
                    {response ? (
                        <div className="space-y-4">
                            {response.items.map((item) => (
                                <div
                                    key={item.track_id}
                                    className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4"
                                >
                                    <div className="flex flex-wrap items-start justify-between gap-3">
                                        <div>
                                            <div className="flex items-center gap-3">
                                                <span className="flex h-9 w-9 items-center justify-center rounded-full bg-hud-accent-primary/10 text-sm font-semibold text-hud-accent-primary">
                                                    {item.rank}
                                                </span>
                                                <div>
                                                    <p className="text-base font-semibold text-hud-text-primary">{item.title}</p>
                                                    <p className="mt-1 text-sm text-hud-text-secondary">{item.artist_name}</p>
                                                </div>
                                            </div>
                                            {item.reason && (
                                                <p className="mt-4 text-sm leading-6 text-hud-text-secondary">{item.reason}</p>
                                            )}
                                        </div>

                                        <div className="rounded-2xl border border-hud-border-secondary bg-hud-bg-secondary/80 px-4 py-3 text-right">
                                            <p className="text-xs uppercase tracking-[0.22em] text-hud-text-muted">Score</p>
                                            <p className="mt-1 text-xl font-semibold text-hud-text-primary">
                                                {item.score.toFixed(2)}
                                            </p>
                                        </div>
                                    </div>

                                    <div className="mt-4 flex flex-wrap gap-2">
                                        <span className="rounded-full border border-hud-border-primary bg-hud-accent-primary/10 px-3 py-1 text-xs font-medium text-hud-accent-primary">
                                            {item.track_id}
                                        </span>
                                        <span className="rounded-full border border-hud-border-secondary px-3 py-1 text-xs font-medium text-hud-text-secondary">
                                            source: {item.source_space}
                                        </span>
                                        <span className="rounded-full border border-hud-border-secondary px-3 py-1 text-xs font-medium text-hud-text-secondary">
                                            energy: {item.energy_level}
                                        </span>
                                    </div>
                                </div>
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
        </div>
    )
}

export default GmsPreviewPage
