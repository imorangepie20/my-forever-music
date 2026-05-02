import { startTransition, useEffect, useState } from 'react'
import { Gauge, HeartPulse, RefreshCw, SlidersHorizontal, Sparkles } from 'lucide-react'
import { Link } from 'react-router-dom'
import Button from '@/components/common/Button'
import HudCard from '@/components/common/HudCard'
import { useRecommendationWorkspace } from '@/contexts/RecommendationWorkspaceContext'
import { analyzeEmsWorkspace, ApiError } from '@/services/api'
import type { EmsWorkspaceAnalysisResponse } from '@/types/api'
import type { WorkspaceMood } from '@/types/workspace'

const moods: Array<{ value: WorkspaceMood; label: string; description: string }> = [
    { value: 'focus', label: 'Focus', description: 'Sharper energy with lower noise and distraction.' },
    { value: 'calm', label: 'Calm', description: 'Soft pacing for long listening sessions.' },
    { value: 'upbeat', label: 'Upbeat', description: 'Higher motion and more immediate lift.' },
    { value: 'melancholy', label: 'Melancholy', description: 'Reflective tone with lower emotional brightness.' },
    { value: 'discovery', label: 'Discovery', description: 'Less predictable mood shaping and broader variety.' },
]

const splitField = (value: string) =>
    value
        .split(',')
        .map((item) => item.trim())
        .filter(Boolean)

const EmsPage = () => {
    const { workspace, updateWorkspace } = useRecommendationWorkspace()
    const [analysis, setAnalysis] = useState<EmsWorkspaceAnalysisResponse | null>(null)
    const [isLoading, setIsLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        const controller = new AbortController()

        setIsLoading(true)
        setError(null)

        analyzeEmsWorkspace(
            {
                user_id: workspace.userId || undefined,
                playlist_id: workspace.playlistId || undefined,
                seed_track_ids: splitField(workspace.seedTrackIdsText),
                seed_artist_names: splitField(workspace.seedArtistNamesText),
                seed_genres: splitField(workspace.seedGenresText),
            },
            controller.signal,
        )
            .then((response) => {
                startTransition(() => {
                    setAnalysis(response)
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
                        : 'Unable to analyze the EMS workspace from the Spring Boot API.'

                startTransition(() => {
                    setAnalysis(null)
                    setError(message)
                })
            })
            .finally(() => {
                setIsLoading(false)
            })

        return () => controller.abort()
    }, [
        workspace.playlistId,
        workspace.seedArtistNamesText,
        workspace.seedGenresText,
        workspace.seedTrackIdsText,
        workspace.userId,
    ])

    const applyRecommendation = () => {
        if (!analysis) {
            return
        }

        updateWorkspace({
            mood: analysis.workspace_recommendation.mood,
            energyLevel: analysis.workspace_recommendation.energy_level,
            familiarityBias: analysis.workspace_recommendation.familiarity_bias,
        })
    }

    return (
        <div className="space-y-6">
            <section className="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
                <HudCard title="EMS Signal Tuning" subtitle="Shape the listening context before GMS ranking">
                    <div className="grid gap-3 md:grid-cols-2">
                        {moods.map((mood) => (
                            <button
                                key={mood.value}
                                type="button"
                                onClick={() => updateWorkspace({ mood: mood.value })}
                                className={`rounded-2xl border p-4 text-left transition-hud ${
                                    workspace.mood === mood.value
                                        ? 'border-hud-border-primary bg-hud-accent-primary/10'
                                        : 'border-hud-border-secondary bg-hud-bg-primary/70 hover:border-hud-border-primary'
                                }`}
                            >
                                <p className="text-sm font-semibold text-hud-text-primary">{mood.label}</p>
                                <p className="mt-2 text-sm leading-6 text-hud-text-secondary">{mood.description}</p>
                            </button>
                        ))}
                    </div>

                    <div className="mt-6 grid gap-5 md:grid-cols-2">
                        <div>
                            <label className="mb-2 block text-sm font-medium text-hud-text-secondary">
                                Energy Level: {workspace.energyLevel}
                            </label>
                            <input
                                type="range"
                                min="1"
                                max="5"
                                value={workspace.energyLevel}
                                onChange={(event) => updateWorkspace({ energyLevel: Number(event.target.value) })}
                                className="w-full accent-hud-accent-primary"
                            />
                        </div>
                        <div>
                            <label className="mb-2 block text-sm font-medium text-hud-text-secondary">
                                Familiarity Bias: {workspace.familiarityBias}
                            </label>
                            <input
                                type="range"
                                min="1"
                                max="5"
                                value={workspace.familiarityBias}
                                onChange={(event) => updateWorkspace({ familiarityBias: Number(event.target.value) })}
                                className="w-full accent-hud-accent-primary"
                            />
                        </div>
                    </div>

                    <div className="mt-5">
                        <label className="mb-2 block text-sm font-medium text-hud-text-secondary">
                            Preview Limit
                        </label>
                        <input
                            type="number"
                            min="1"
                            max="20"
                            value={workspace.limit}
                            onChange={(event) => updateWorkspace({ limit: Number(event.target.value) })}
                            className="w-full rounded-xl border border-hud-border-secondary bg-hud-bg-primary px-4 py-3 text-sm text-hud-text-primary outline-none transition-hud focus:border-hud-border-primary"
                        />
                    </div>

                    <label className="mt-5 flex items-center gap-3 rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 px-4 py-3 text-sm text-hud-text-secondary">
                        <input
                            type="checkbox"
                            checked={workspace.includeExplanations}
                            onChange={(event) =>
                                updateWorkspace({ includeExplanations: event.target.checked })
                            }
                            className="h-4 w-4 rounded border-hud-border-secondary bg-hud-bg-primary text-hud-accent-primary"
                        />
                        Ask the AI preview to include explanation strings for each recommendation candidate
                    </label>

                    <div className="mt-6 flex flex-wrap gap-3">
                        <Button type="button" variant="ghost" onClick={applyRecommendation} disabled={!analysis}>
                            Apply API Recommendation
                        </Button>
                        <Link to="/pms">
                            <Button type="button" variant="outline">
                                Back to PMS
                            </Button>
                        </Link>
                        <Link to="/gms-preview">
                            <Button type="button" variant="primary" glow>
                                Continue to GMS
                            </Button>
                        </Link>
                    </div>
                </HudCard>

                <div className="space-y-6">
                    <HudCard
                        title="EMS Summary"
                        subtitle="Signals that will be forwarded to GMS"
                        action={
                            isLoading ? (
                                <span className="inline-flex items-center gap-2 text-xs text-hud-text-muted">
                                    <RefreshCw size={14} className="animate-spin" />
                                    Analyzing
                                </span>
                            ) : null
                        }
                    >
                        <div className="space-y-4">
                            <div className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                                <div className="flex items-center gap-3">
                                    <span className="rounded-xl bg-hud-accent-primary/10 p-2 text-hud-accent-primary">
                                        <HeartPulse size={18} />
                                    </span>
                                    <div>
                                        <p className="text-xs uppercase tracking-[0.22em] text-hud-text-muted">
                                            Mood
                                        </p>
                                        <p className="mt-1 text-2xl font-semibold capitalize text-hud-text-primary">
                                            {workspace.mood}
                                        </p>
                                    </div>
                                </div>
                            </div>
                            <div className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                                <div className="flex items-center gap-3">
                                    <span className="rounded-xl bg-hud-accent-info/10 p-2 text-hud-accent-info">
                                        <Gauge size={18} />
                                    </span>
                                    <div>
                                        <p className="text-xs uppercase tracking-[0.22em] text-hud-text-muted">
                                            Energy / Bias
                                        </p>
                                        <p className="mt-1 text-2xl font-semibold text-hud-text-primary">
                                            {workspace.energyLevel} / {workspace.familiarityBias}
                                        </p>
                                    </div>
                                </div>
                            </div>
                            <div className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                                <div className="flex items-center gap-3">
                                    <span className="rounded-xl bg-hud-accent-warning/10 p-2 text-hud-accent-warning">
                                        <SlidersHorizontal size={18} />
                                    </span>
                                    <div>
                                        <p className="text-xs uppercase tracking-[0.22em] text-hud-text-muted">
                                            Response Shape
                                        </p>
                                        <p className="mt-1 text-sm leading-6 text-hud-text-primary">
                                            {workspace.limit} items, explanations{' '}
                                            {workspace.includeExplanations ? 'enabled' : 'disabled'}
                                        </p>
                                    </div>
                                </div>
                            </div>
                            {analysis && (
                                <div className="rounded-2xl border border-hud-border-primary bg-hud-accent-primary/10 p-4">
                                    <div className="flex items-center gap-3">
                                        <span className="rounded-xl bg-hud-accent-primary/20 p-2 text-hud-accent-primary">
                                            <Sparkles size={18} />
                                        </span>
                                        <div>
                                            <p className="text-xs uppercase tracking-[0.22em] text-hud-text-muted">
                                                API Recommendation
                                            </p>
                                            <p className="mt-1 text-sm font-medium text-hud-text-primary">
                                                {analysis.workspace_recommendation.mood} mood, energy{' '}
                                                {analysis.workspace_recommendation.energy_level}, bias{' '}
                                                {analysis.workspace_recommendation.familiarity_bias}
                                            </p>
                                            <p className="mt-2 text-xs text-hud-text-secondary">
                                                Confidence {analysis.workspace_recommendation.confidence_score}
                                            </p>
                                        </div>
                                    </div>
                                </div>
                            )}
                        </div>
                    </HudCard>

                    <HudCard title="EMS Analysis Feed" subtitle="What the API inferred from PMS seeds">
                        {error ? (
                            <div className="rounded-2xl border border-hud-accent-danger/40 bg-hud-accent-danger/10 p-4 text-sm leading-6 text-hud-text-secondary">
                                {error}
                            </div>
                        ) : analysis ? (
                            <div className="space-y-4">
                                <div className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                                    <p className="text-xs uppercase tracking-[0.22em] text-hud-text-muted">
                                        Strategy
                                    </p>
                                    <p className="mt-2 text-sm font-medium text-hud-text-primary">
                                        {analysis.context.strategy}
                                    </p>
                                    <p className="mt-2 text-xs text-hud-text-secondary">
                                        Matched catalog tracks: {analysis.context.matched_catalog_track_count}
                                    </p>
                                </div>

                                <div className="space-y-3">
                                    {analysis.top_signals.map((signal) => (
                                        <div
                                            key={`${signal.type}-${signal.label}`}
                                            className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4"
                                        >
                                            <div className="flex items-center justify-between gap-3">
                                                <p className="text-sm font-semibold text-hud-text-primary">
                                                    {signal.label}
                                                </p>
                                                <span className="rounded-full border border-hud-border-secondary px-3 py-1 text-xs uppercase tracking-[0.2em] text-hud-text-muted">
                                                    {signal.type} · {signal.weight}
                                                </span>
                                            </div>
                                            <p className="mt-2 text-sm leading-6 text-hud-text-secondary">
                                                {signal.reason}
                                            </p>
                                        </div>
                                    ))}
                                </div>

                                <div className="space-y-3 text-sm leading-6 text-hud-text-secondary">
                                    {analysis.notes.map((note) => (
                                        <p key={note}>{note}</p>
                                    ))}
                                </div>

                                {analysis.warnings.length > 0 && (
                                    <div className="rounded-2xl border border-hud-accent-warning/40 bg-hud-accent-warning/10 p-4">
                                        <p className="text-xs uppercase tracking-[0.22em] text-hud-accent-warning">
                                            Warnings
                                        </p>
                                        <div className="mt-3 space-y-2 text-sm leading-6 text-hud-text-secondary">
                                            {analysis.warnings.map((warning) => (
                                                <p key={warning}>{warning}</p>
                                            ))}
                                        </div>
                                    </div>
                                )}
                            </div>
                        ) : (
                            <div className="text-sm leading-6 text-hud-text-secondary">
                                EMS analysis will appear here after the PMS workspace is evaluated.
                            </div>
                        )}
                    </HudCard>
                </div>
            </section>
        </div>
    )
}

export default EmsPage
