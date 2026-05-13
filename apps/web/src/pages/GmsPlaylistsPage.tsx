import { useCallback, useEffect, useState } from 'react'
import { ArrowDownToLine, CheckCircle2, ExternalLink, RefreshCw, Sparkles } from 'lucide-react'
import Button from '@/components/common/Button'
import ConfirmDialog from '@/components/common/ConfirmDialog'
import HudCard from '@/components/common/HudCard'
import { useAuthSession } from '@/contexts/AuthSessionContext'
import { ApiError, fetchGmsPlaylistPreview, saveGmsPlaylistToPms } from '@/services/api'
import type { GmsPlaylistPreviewItem, GmsPlaylistPreviewResponse, GmsPlaylistSaveResponse } from '@/types/api'

const DEFAULT_LIMIT = 12

const formatScore = (value: number) => value.toFixed(2)

const formatCollectedAt = (value: string) => {
    try {
        return new Date(value).toLocaleString()
    } catch {
        return value
    }
}

const GmsPlaylistsPage = () => {
    const { session } = useAuthSession()
    const userId = session?.userId ?? ''

    const [preview, setPreview] = useState<GmsPlaylistPreviewResponse | null>(null)
    const [isLoading, setIsLoading] = useState(false)
    const [errorMessage, setErrorMessage] = useState<string | null>(null)
    const [pendingSaveId, setPendingSaveId] = useState<number | null>(null)
    const [confirmCandidate, setConfirmCandidate] = useState<GmsPlaylistPreviewItem | null>(null)
    const [lastSaveResult, setLastSaveResult] = useState<GmsPlaylistSaveResponse | null>(null)
    const [savedPlaylistIds, setSavedPlaylistIds] = useState<Set<number>>(new Set())

    const loadPreview = useCallback(
        (signal?: AbortSignal) => {
            if (!userId) {
                setPreview(null)
                setErrorMessage('Sign in to load GMS playlist candidates.')
                return
            }

            setIsLoading(true)
            setErrorMessage(null)

            fetchGmsPlaylistPreview(userId, DEFAULT_LIMIT, signal)
                .then((response) => {
                    setPreview(response)
                })
                .catch((requestError: unknown) => {
                    if (requestError instanceof DOMException && requestError.name === 'AbortError') {
                        return
                    }
                    const message =
                        requestError instanceof ApiError
                            ? requestError.message
                            : 'Unable to load GMS playlist candidates.'
                    setErrorMessage(message)
                    setPreview(null)
                })
                .finally(() => {
                    setIsLoading(false)
                })
        },
        [userId],
    )

    useEffect(() => {
        const controller = new AbortController()
        loadPreview(controller.signal)
        return () => controller.abort()
    }, [loadPreview])

    const handleSaveConfirmed = async () => {
        if (!userId || !confirmCandidate) {
            return
        }

        setPendingSaveId(confirmCandidate.playlist_id)
        setErrorMessage(null)

        try {
            const result = await saveGmsPlaylistToPms(confirmCandidate.playlist_id, userId, {
                title: null,
            })
            setLastSaveResult(result)
            setSavedPlaylistIds((current) => {
                const next = new Set(current)
                next.add(confirmCandidate.playlist_id)
                return next
            })
            setConfirmCandidate(null)
        } catch (requestError: unknown) {
            const message =
                requestError instanceof ApiError
                    ? requestError.message
                    : 'Unable to save this EMS playlist into PMS.'
            setErrorMessage(message)
        } finally {
            setPendingSaveId(null)
        }
    }

    const candidates = preview?.candidates ?? []

    return (
        <div className="space-y-6">
            <HudCard
                title="GMS Playlist Candidates"
                subtitle="EMS에서 평가한 공개 플레이리스트 중 사용자의 PMS 라이브러리와 연관도가 높은 후보"
                action={
                    <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        disabled={isLoading || !userId}
                        onClick={() => loadPreview()}
                    >
                        <RefreshCw size={14} className={isLoading ? 'animate-spin' : ''} />
                        Refresh
                    </Button>
                }
            >
                <div className="grid gap-3 md:grid-cols-3">
                    <div className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                        <p className="text-[11px] uppercase tracking-[0.24em] text-hud-text-muted">User ID</p>
                        <p className="mt-2 text-sm font-medium text-hud-text-primary">
                            {userId || 'Not signed in'}
                        </p>
                    </div>
                    <div className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                        <p className="text-[11px] uppercase tracking-[0.24em] text-hud-text-muted">Model Stage</p>
                        <p className="mt-2 text-sm font-medium capitalize text-hud-text-primary">
                            {preview?.model_stage ?? '—'}
                        </p>
                    </div>
                    <div className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                        <p className="text-[11px] uppercase tracking-[0.24em] text-hud-text-muted">Preferred Platform</p>
                        <p className="mt-2 text-sm font-medium capitalize text-hud-text-primary">
                            {preview?.preferred_platform ?? 'any'}
                        </p>
                    </div>
                </div>

                {errorMessage && (
                    <div className="mt-4 rounded-2xl border border-hud-accent-danger/40 bg-hud-accent-danger/10 p-4 text-sm leading-6 text-hud-text-secondary">
                        {errorMessage}
                    </div>
                )}

                {lastSaveResult && (
                    <div className="mt-4 flex items-start gap-3 rounded-2xl border border-hud-accent-primary/40 bg-hud-accent-primary/10 p-4 text-sm leading-6 text-hud-text-secondary">
                        <CheckCircle2 size={18} className="mt-0.5 text-hud-accent-primary" />
                        <div>
                            <p className="font-medium text-hud-text-primary">
                                Saved to PMS: {lastSaveResult.personal_playlist_title}
                            </p>
                            <p className="mt-1 text-xs text-hud-text-muted">
                                Added {lastSaveResult.added_track_count} track(s) ·{' '}
                                {lastSaveResult.personal_playlist_track_count} total in this playlist
                            </p>
                        </div>
                    </div>
                )}
            </HudCard>

            <HudCard title="Candidate Playlists" subtitle="Affinity 점수가 높은 순으로 정렬됨">
                {!preview && isLoading && (
                    <div className="flex items-center gap-3 rounded-2xl border border-dashed border-hud-border-secondary bg-hud-bg-primary/60 p-6 text-sm text-hud-text-secondary">
                        <RefreshCw size={18} className="animate-spin text-hud-accent-primary" />
                        Loading GMS playlist candidates...
                    </div>
                )}

                {preview && candidates.length === 0 && (
                    <div className="flex items-center gap-3 rounded-2xl border border-dashed border-hud-border-secondary bg-hud-bg-primary/60 p-6 text-sm text-hud-text-secondary">
                        <Sparkles size={18} className="text-hud-accent-primary" />
                        후보 플레이리스트가 없습니다. EMS pool 적재가 더 채워질 때까지 기다리거나, 본인 PMS 라이브러리를 확장하세요.
                    </div>
                )}

                {candidates.length > 0 && (
                    <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                        {candidates.map((candidate) => {
                            const isSaving = pendingSaveId === candidate.playlist_id
                            const alreadySaved = savedPlaylistIds.has(candidate.playlist_id)
                            return (
                                <div
                                    key={candidate.playlist_id}
                                    className="flex flex-col gap-3 rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4"
                                >
                                    {candidate.cover_image_url ? (
                                        <img
                                            src={candidate.cover_image_url}
                                            alt={candidate.title}
                                            className="aspect-[4/3] w-full rounded-xl object-cover"
                                        />
                                    ) : (
                                        <div className="flex aspect-[4/3] w-full items-center justify-center rounded-xl bg-hud-bg-secondary/60 text-hud-text-muted">
                                            <Sparkles size={28} />
                                        </div>
                                    )}

                                    <div className="space-y-1">
                                        <p className="text-sm font-semibold text-hud-text-primary line-clamp-2">
                                            {candidate.title}
                                        </p>
                                        <p className="text-xs text-hud-text-muted">
                                            {candidate.curator || 'Unknown curator'} · {candidate.source_platform}
                                        </p>
                                        {candidate.description && (
                                            <p className="text-xs leading-5 text-hud-text-secondary line-clamp-3">
                                                {candidate.description}
                                            </p>
                                        )}
                                    </div>

                                    <div className="grid grid-cols-2 gap-2 text-[11px]">
                                        <div className="rounded-xl border border-hud-border-secondary bg-hud-bg-secondary/60 px-2 py-2">
                                            <p className="uppercase tracking-[0.18em] text-hud-text-muted">Affinity</p>
                                            <p className="mt-1 font-semibold text-hud-accent-primary">
                                                {formatScore(candidate.affinity_score)}
                                            </p>
                                        </div>
                                        <div className="rounded-xl border border-hud-border-secondary bg-hud-bg-secondary/60 px-2 py-2">
                                            <p className="uppercase tracking-[0.18em] text-hud-text-muted">Confidence</p>
                                            <p className="mt-1 font-semibold text-hud-text-primary">
                                                {formatScore(candidate.confidence_score)}
                                            </p>
                                        </div>
                                        <div className="rounded-xl border border-hud-border-secondary bg-hud-bg-secondary/60 px-2 py-2">
                                            <p className="uppercase tracking-[0.18em] text-hud-text-muted">Tracks</p>
                                            <p className="mt-1 font-semibold text-hud-text-primary">
                                                {candidate.track_count}
                                            </p>
                                        </div>
                                        <div className="rounded-xl border border-hud-border-secondary bg-hud-bg-secondary/60 px-2 py-2">
                                            <p className="uppercase tracking-[0.18em] text-hud-text-muted">Audio Feat</p>
                                            <p className="mt-1 font-semibold text-hud-text-primary">
                                                {candidate.audio_feature_filled_count}
                                            </p>
                                        </div>
                                    </div>

                                    <p className="text-[11px] text-hud-text-muted">
                                        Collected {formatCollectedAt(candidate.collected_at)}
                                    </p>

                                    <div className="mt-auto flex gap-2">
                                        {candidate.platform_external_url && (
                                            <a
                                                href={candidate.platform_external_url}
                                                target="_blank"
                                                rel="noopener noreferrer"
                                                className="inline-flex items-center justify-center gap-1.5 rounded-lg border border-hud-border-secondary px-3 py-1.5 text-xs text-hud-text-secondary transition-hud hover:border-hud-border-primary hover:text-hud-text-primary"
                                            >
                                                <ExternalLink size={14} />
                                                Open
                                            </a>
                                        )}
                                        <Button
                                            type="button"
                                            variant={alreadySaved ? 'ghost' : 'primary'}
                                            size="sm"
                                            fullWidth
                                            disabled={isSaving || alreadySaved}
                                            onClick={() => setConfirmCandidate(candidate)}
                                        >
                                            {isSaving ? (
                                                <>
                                                    <RefreshCw size={14} className="animate-spin" />
                                                    Saving
                                                </>
                                            ) : alreadySaved ? (
                                                <>
                                                    <CheckCircle2 size={14} />
                                                    Saved
                                                </>
                                            ) : (
                                                <>
                                                    <ArrowDownToLine size={14} />
                                                    Save to PMS
                                                </>
                                            )}
                                        </Button>
                                    </div>
                                </div>
                            )
                        })}
                    </div>
                )}
            </HudCard>

            <ConfirmDialog
                open={confirmCandidate !== null}
                title="이 플레이리스트를 PMS에 저장할까요?"
                description={
                    confirmCandidate
                        ? `"${confirmCandidate.title}"의 ${confirmCandidate.track_count}곡이 개인 라이브러리에 추가됩니다.`
                        : ''
                }
                confirmLabel="저장"
                cancelLabel="취소"
                variant="primary"
                loading={pendingSaveId !== null}
                onConfirm={handleSaveConfirmed}
                onCancel={() => setConfirmCandidate(null)}
            />
        </div>
    )
}

export default GmsPlaylistsPage
