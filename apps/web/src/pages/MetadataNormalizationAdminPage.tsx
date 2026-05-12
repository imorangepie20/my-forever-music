import { useCallback, useEffect, useMemo, useState } from 'react'
import { AlertTriangle, BadgeCheck, Check, Compass, RefreshCw, Search, ShieldCheck, X } from 'lucide-react'
import Button from '@/components/common/Button'
import ConfirmDialog from '@/components/common/ConfirmDialog'
import { useAuthSession } from '@/contexts/AuthSessionContext'
import {
    acceptMetadataCandidateForAdmin,
    autoAcceptMetadataCandidatesForAdmin,
    listMetadataCandidatesForAdmin,
    lookupMusicBrainzRecordingsForAdmin,
    rejectMetadataCandidateForAdmin,
} from '@/services/api'
import type { MetadataLookupResponse, TrackIdentityCandidateItem } from '@/types/api'

const ADMIN_EMAIL = 'jowoosungtidal@gmail.com'

type StatusFilter = 'pending' | 'accepted' | 'rejected' | 'all'

const STATUS_FILTERS: { value: StatusFilter; label: string }[] = [
    { value: 'pending', label: 'Pending' },
    { value: 'accepted', label: 'Accepted' },
    { value: 'rejected', label: 'Rejected' },
    { value: 'all', label: 'All' },
]

const statusTone: Record<string, string> = {
    pending: 'border-amber-300/40 bg-amber-300/10 text-amber-100',
    accepted: 'border-emerald-300/40 bg-emerald-300/10 text-emerald-100',
    rejected: 'border-rose-300/40 bg-rose-300/10 text-rose-100',
}

const formatDateTime = (value: string | null) => {
    if (!value) {
        return '-'
    }
    return new Intl.DateTimeFormat('ko-KR', {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
    }).format(new Date(value))
}

type PendingResolution =
    | { kind: 'accept'; candidate: TrackIdentityCandidateItem }
    | { kind: 'reject'; candidate: TrackIdentityCandidateItem }
    | { kind: 'autoAccept'; minScore: number; limit: number }

const MetadataNormalizationAdminPage = () => {
    const { session } = useAuthSession()
    const isAdmin = session?.email.toLowerCase() === ADMIN_EMAIL

    const [title, setTitle] = useState('')
    const [artist, setArtist] = useState('')
    const [limit, setLimit] = useState(10)
    const [persist, setPersist] = useState(true)
    const [lookupResult, setLookupResult] = useState<MetadataLookupResponse | null>(null)
    const [lookupLoading, setLookupLoading] = useState(false)
    const [lookupError, setLookupError] = useState<string | null>(null)

    const [statusFilter, setStatusFilter] = useState<StatusFilter>('pending')
    const [candidates, setCandidates] = useState<TrackIdentityCandidateItem[]>([])
    const [candidatesLoading, setCandidatesLoading] = useState(false)
    const [candidatesError, setCandidatesError] = useState<string | null>(null)
    const [pendingResolution, setPendingResolution] = useState<PendingResolution | null>(null)
    const [resolving, setResolving] = useState(false)
    const [resolveNotes, setResolveNotes] = useState('')
    const [autoAcceptMinScore, setAutoAcceptMinScore] = useState(0.95)
    const [autoAcceptSummary, setAutoAcceptSummary] = useState<string | null>(null)

    const loadCandidates = useCallback(async (signal?: AbortSignal) => {
        if (!session || !isAdmin) {
            return
        }
        setCandidatesLoading(true)
        setCandidatesError(null)
        try {
            const status = statusFilter === 'all' ? undefined : statusFilter
            const response = await listMetadataCandidatesForAdmin(session.userId, status, 50, signal)
            setCandidates(response.candidates)
        } catch (err) {
            if (signal?.aborted) {
                return
            }
            setCandidatesError(err instanceof Error ? err.message : '메타데이터 candidate 목록을 불러오지 못했습니다.')
        } finally {
            setCandidatesLoading(false)
        }
    }, [isAdmin, session, statusFilter])

    useEffect(() => {
        const controller = new AbortController()
        void loadCandidates(controller.signal)
        return () => controller.abort()
    }, [loadCandidates])

    const handleLookup = async () => {
        if (!session || !title.trim()) {
            return
        }
        setLookupLoading(true)
        setLookupError(null)
        try {
            const response = await lookupMusicBrainzRecordingsForAdmin(
                session.userId,
                title.trim(),
                artist.trim() || undefined,
                Math.max(1, Math.min(25, limit)),
                persist,
            )
            setLookupResult(response)
            if (persist) {
                await loadCandidates()
            }
        } catch (err) {
            setLookupError(err instanceof Error ? err.message : 'MusicBrainz lookup 이 실패했습니다.')
        } finally {
            setLookupLoading(false)
        }
    }

    const handleResolutionConfirm = async () => {
        if (!session || !pendingResolution) {
            return
        }
        setResolving(true)
        try {
            if (pendingResolution.kind === 'autoAccept') {
                const result = await autoAcceptMetadataCandidatesForAdmin(
                    session.userId,
                    pendingResolution.minScore,
                    pendingResolution.limit,
                )
                setAutoAcceptSummary(
                    `threshold ${result.threshold.toFixed(2)} — reviewed ${result.reviewed_count}, accepted ${result.accepted_count}, skipped ${result.skipped_count}.`,
                )
            } else {
                const candidateId = pendingResolution.candidate.id
                if (pendingResolution.kind === 'accept') {
                    await acceptMetadataCandidateForAdmin(session.userId, candidateId, resolveNotes.trim() || null)
                } else {
                    await rejectMetadataCandidateForAdmin(session.userId, candidateId, resolveNotes.trim() || null)
                }
            }
            setPendingResolution(null)
            setResolveNotes('')
            await loadCandidates()
        } catch (err) {
            setCandidatesError(err instanceof Error ? err.message : 'Candidate 처리에 실패했습니다.')
        } finally {
            setResolving(false)
        }
    }

    const dialogTitle = useMemo(() => {
        if (!pendingResolution) {
            return ''
        }
        if (pendingResolution.kind === 'autoAccept') {
            return `Auto-accept pending candidates (score >= ${pendingResolution.minScore.toFixed(2)})`
        }
        return pendingResolution.kind === 'accept'
            ? `Accept candidate #${pendingResolution.candidate.id}`
            : `Reject candidate #${pendingResolution.candidate.id}`
    }, [pendingResolution])

    const dialogDescription = useMemo(() => {
        if (!pendingResolution) {
            return undefined
        }
        if (pendingResolution.kind === 'autoAccept') {
            return `최근 pending candidate 최대 ${pendingResolution.limit}건 중\ncandidate_score >= ${pendingResolution.minScore.toFixed(2)} 인 후보를 한꺼번에 accept 합니다.`
        }
        return `${pendingResolution.candidate.source}/${pendingResolution.candidate.candidate_kind} = ${pendingResolution.candidate.candidate_value}\nquery: ${pendingResolution.candidate.query_title}${pendingResolution.candidate.query_artist ? ` / ${pendingResolution.candidate.query_artist}` : ''}`
    }, [pendingResolution])

    const dialogConfirmLabel = useMemo(() => {
        if (!pendingResolution) {
            return ''
        }
        if (pendingResolution.kind === 'autoAccept') return 'Auto-accept'
        return pendingResolution.kind === 'accept' ? 'Accept' : 'Reject'
    }, [pendingResolution])

    const dialogVariant = useMemo((): 'primary' | 'danger' => {
        if (!pendingResolution) return 'primary'
        return pendingResolution.kind === 'reject' ? 'danger' : 'primary'
    }, [pendingResolution])

    if (!session || !isAdmin) {
        return (
            <main className="space-y-6">
                <section className="rounded-2xl border border-hud-border-secondary bg-hud-bg-secondary/80 p-6">
                    <div className="flex items-center gap-3 text-amber-100">
                        <ShieldCheck size={22} />
                        <h2 className="text-xl font-semibold">Metadata Normalization Admin</h2>
                    </div>
                    <p className="mt-4 text-sm leading-6 text-hud-text-secondary">
                        이 화면은 {ADMIN_EMAIL} 관리자 계정에만 노출됩니다.
                    </p>
                </section>
            </main>
        )
    }

    return (
        <main className="space-y-6">
            <section className="rounded-2xl border border-hud-border-secondary bg-hud-bg-secondary/85 p-6">
                <div className="flex items-center gap-3 text-hud-accent-primary">
                    <Compass size={24} />
                    <p className="text-xs font-semibold uppercase tracking-[0.26em]">Metadata Normalization</p>
                </div>
                <h2 className="mt-3 text-2xl font-semibold text-hud-text-primary">
                    MusicBrainz 후보 lookup + review
                </h2>
                <p className="mt-2 text-sm text-hud-text-secondary">
                    title + artist 로 recording 후보(MBID/ISRC)를 조회하고, persist 옵션이 켜져 있으면 각 후보를
                    `track_identity_candidate` 에 저장합니다. 운영자는 아래 candidate 목록에서 accept/reject 합니다.
                </p>
            </section>

            <section className="rounded-2xl border border-hud-border-secondary bg-hud-bg-secondary/80 p-6">
                <div className="grid gap-3 lg:grid-cols-[2fr_2fr_120px_140px_auto]">
                    <input
                        type="text"
                        value={title}
                        onChange={(e) => setTitle(e.target.value)}
                        placeholder="Track title"
                        className="rounded-lg border border-hud-border-secondary bg-hud-bg-primary/60 px-3 py-2 text-sm text-hud-text-primary focus:border-hud-border-primary focus:outline-none"
                    />
                    <input
                        type="text"
                        value={artist}
                        onChange={(e) => setArtist(e.target.value)}
                        placeholder="Artist (optional)"
                        className="rounded-lg border border-hud-border-secondary bg-hud-bg-primary/60 px-3 py-2 text-sm text-hud-text-primary focus:border-hud-border-primary focus:outline-none"
                    />
                    <input
                        type="number"
                        value={limit}
                        min={1}
                        max={25}
                        onChange={(e) => setLimit(Number(e.target.value) || 10)}
                        className="rounded-lg border border-hud-border-secondary bg-hud-bg-primary/60 px-3 py-2 text-sm text-hud-text-primary focus:border-hud-border-primary focus:outline-none"
                    />
                    <label className="flex items-center gap-2 rounded-lg border border-hud-border-secondary bg-hud-bg-primary/60 px-3 py-2 text-sm text-hud-text-primary">
                        <input
                            type="checkbox"
                            checked={persist}
                            onChange={(e) => setPersist(e.target.checked)}
                        />
                        Persist
                    </label>
                    <Button
                        type="button"
                        variant="primary"
                        onClick={() => void handleLookup()}
                        disabled={lookupLoading || !title.trim()}
                    >
                        <Search size={16} />
                        Lookup
                    </Button>
                </div>
                {lookupError && (
                    <div className="mt-4 flex items-start gap-3 rounded-xl border border-rose-300/30 bg-rose-500/10 p-4 text-sm text-rose-100">
                        <AlertTriangle size={18} />
                        <span>{lookupError}</span>
                    </div>
                )}
                {lookupResult && (
                    <div className="mt-5 overflow-hidden rounded-xl border border-hud-border-secondary">
                        <table className="w-full min-w-[820px] text-left text-sm">
                            <thead className="bg-hud-bg-primary/80 text-xs uppercase tracking-[0.18em] text-hud-text-muted">
                                <tr>
                                    <th className="px-4 py-3">MBID</th>
                                    <th className="px-4 py-3">Title</th>
                                    <th className="px-4 py-3">Artist</th>
                                    <th className="px-4 py-3">Score</th>
                                    <th className="px-4 py-3">ISRCs</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-hud-border-secondary">
                                {lookupResult.candidates.map((candidate, idx) => (
                                    <tr key={`${candidate.mbid}-${idx}`} className="bg-hud-bg-secondary/40">
                                        <td className="px-4 py-3 font-mono text-xs text-hud-text-secondary">{candidate.mbid ?? '-'}</td>
                                        <td className="px-4 py-3 text-hud-text-primary">{candidate.title ?? '-'}</td>
                                        <td className="px-4 py-3 text-hud-text-secondary">{candidate.artist_name ?? '-'}</td>
                                        <td className="px-4 py-3 text-hud-text-secondary">{candidate.score ?? '-'}</td>
                                        <td className="px-4 py-3 text-xs text-hud-text-secondary">
                                            {candidate.isrcs.length === 0 ? '-' : candidate.isrcs.join(', ')}
                                        </td>
                                    </tr>
                                ))}
                                {lookupResult.candidates.length === 0 && (
                                    <tr>
                                        <td colSpan={5} className="px-4 py-6 text-center text-sm text-hud-text-muted">
                                            검색 결과가 없습니다.
                                        </td>
                                    </tr>
                                )}
                            </tbody>
                        </table>
                    </div>
                )}
            </section>

            <section className="rounded-2xl border border-hud-border-secondary bg-hud-bg-secondary/80 p-6">
                <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                    <div>
                        <p className="text-xs uppercase tracking-[0.22em] text-hud-text-muted">Candidates</p>
                        <h3 className="mt-2 text-lg font-semibold text-hud-text-primary">저장된 candidate</h3>
                    </div>
                    <div className="flex flex-wrap items-center gap-2">
                        {STATUS_FILTERS.map((f) => (
                            <button
                                key={f.value}
                                type="button"
                                onClick={() => setStatusFilter(f.value)}
                                className={`rounded-full border px-3 py-1.5 text-xs uppercase tracking-[0.18em] transition-hud ${
                                    statusFilter === f.value
                                        ? 'border-hud-border-primary bg-hud-accent-primary/10 text-hud-accent-primary'
                                        : 'border-hud-border-secondary bg-hud-bg-primary/60 text-hud-text-secondary hover:border-hud-border-primary'
                                }`}
                            >
                                {f.label}
                            </button>
                        ))}
                        <div className="flex items-center gap-2 rounded-full border border-hud-border-secondary bg-hud-bg-primary/60 px-3 py-1.5">
                            <label className="text-[11px] uppercase tracking-[0.18em] text-hud-text-muted" htmlFor="auto-accept-threshold">
                                min
                            </label>
                            <input
                                id="auto-accept-threshold"
                                type="number"
                                min={0}
                                max={1}
                                step={0.05}
                                value={autoAcceptMinScore}
                                onChange={(e) => setAutoAcceptMinScore(Number(e.target.value) || 0)}
                                className="w-16 bg-transparent text-sm text-hud-text-primary focus:outline-none"
                            />
                        </div>
                        <Button
                            type="button"
                            variant="primary"
                            onClick={() => setPendingResolution({ kind: 'autoAccept', minScore: autoAcceptMinScore, limit: 100 })}
                            disabled={resolving}
                        >
                            <BadgeCheck size={16} />
                            Auto-accept
                        </Button>
                        <Button type="button" variant="outline" onClick={() => void loadCandidates()} disabled={candidatesLoading}>
                            <RefreshCw size={16} />
                            새로고침
                        </Button>
                    </div>
                </div>
                {autoAcceptSummary && (
                    <div className="mt-4 flex items-start gap-3 rounded-xl border border-hud-accent-primary/30 bg-hud-accent-primary/10 p-4 text-sm text-hud-text-primary">
                        <BadgeCheck size={18} className="text-hud-accent-primary" />
                        <span>{autoAcceptSummary}</span>
                    </div>
                )}
                {candidatesError && (
                    <div className="mt-4 flex items-start gap-3 rounded-xl border border-rose-300/30 bg-rose-500/10 p-4 text-sm text-rose-100">
                        <AlertTriangle size={18} />
                        <span>{candidatesError}</span>
                    </div>
                )}
                <div className="mt-4 overflow-hidden rounded-xl border border-hud-border-secondary">
                    <table className="w-full min-w-[940px] text-left text-sm">
                        <thead className="bg-hud-bg-primary/80 text-xs uppercase tracking-[0.18em] text-hud-text-muted">
                            <tr>
                                <th className="px-4 py-3">#</th>
                                <th className="px-4 py-3">Query</th>
                                <th className="px-4 py-3">Source</th>
                                <th className="px-4 py-3">Kind</th>
                                <th className="px-4 py-3">Value</th>
                                <th className="px-4 py-3">Score</th>
                                <th className="px-4 py-3">Status</th>
                                <th className="px-4 py-3">Created</th>
                                <th className="px-4 py-3 text-right">Action</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-hud-border-secondary">
                            {candidates.map((candidate) => (
                                <tr key={candidate.id} className="bg-hud-bg-secondary/40 align-top">
                                    <td className="px-4 py-3 text-hud-text-secondary">{candidate.id}</td>
                                    <td className="px-4 py-3">
                                        <p className="text-hud-text-primary">{candidate.query_title}</p>
                                        {candidate.query_artist && (
                                            <p className="mt-1 text-xs text-hud-text-muted">{candidate.query_artist}</p>
                                        )}
                                    </td>
                                    <td className="px-4 py-3 text-hud-text-secondary">{candidate.source}</td>
                                    <td className="px-4 py-3 text-hud-text-secondary">{candidate.candidate_kind}</td>
                                    <td className="px-4 py-3 font-mono text-xs text-hud-text-secondary">{candidate.candidate_value}</td>
                                    <td className="px-4 py-3 text-hud-text-secondary">
                                        {candidate.candidate_score == null ? '-' : candidate.candidate_score.toFixed(2)}
                                    </td>
                                    <td className="px-4 py-3">
                                        <span className={`rounded-full border px-2.5 py-1 text-[11px] ${statusTone[candidate.status] ?? statusTone.pending}`}>
                                            {candidate.status}
                                        </span>
                                    </td>
                                    <td className="px-4 py-3 text-xs text-hud-text-muted">{formatDateTime(candidate.created_at)}</td>
                                    <td className="px-4 py-3 text-right">
                                        {candidate.status === 'pending' && (
                                            <div className="flex justify-end gap-2">
                                                <Button
                                                    type="button"
                                                    variant="outline"
                                                    onClick={() => {
                                                        setResolveNotes('')
                                                        setPendingResolution({ kind: 'accept', candidate })
                                                    }}
                                                >
                                                    <Check size={14} />
                                                    Accept
                                                </Button>
                                                <Button
                                                    type="button"
                                                    variant="outline"
                                                    onClick={() => {
                                                        setResolveNotes('')
                                                        setPendingResolution({ kind: 'reject', candidate })
                                                    }}
                                                >
                                                    <X size={14} />
                                                    Reject
                                                </Button>
                                            </div>
                                        )}
                                    </td>
                                </tr>
                            ))}
                            {candidates.length === 0 && !candidatesLoading && (
                                <tr>
                                    <td colSpan={9} className="px-4 py-8 text-center text-sm text-hud-text-muted">
                                        candidate 가 없습니다. 위의 lookup 으로 추가하세요.
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </section>

            <ConfirmDialog
                open={pendingResolution !== null}
                title={dialogTitle}
                description={dialogDescription}
                confirmLabel={dialogConfirmLabel}
                cancelLabel="취소"
                variant={dialogVariant}
                loading={resolving}
                onConfirm={() => void handleResolutionConfirm()}
                onCancel={() => {
                    if (!resolving) {
                        setPendingResolution(null)
                        setResolveNotes('')
                    }
                }}
            />
        </main>
    )
}

export default MetadataNormalizationAdminPage
