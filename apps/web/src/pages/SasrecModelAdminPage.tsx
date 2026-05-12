import { useCallback, useEffect, useState } from 'react'
import { AlertTriangle, BadgeCheck, BoxSelect, RefreshCw, RotateCcw, ShieldCheck, Undo2 } from 'lucide-react'
import Button from '@/components/common/Button'
import ConfirmDialog from '@/components/common/ConfirmDialog'
import { useAuthSession } from '@/contexts/AuthSessionContext'
import {
    disableSasrecModelForAdmin,
    fetchLatestSasrecModelForAdmin,
    promoteSasrecModelForAdmin,
    rollbackSasrecModelForAdmin,
} from '@/services/api'
import type { SasrecRegistryAdminResponse } from '@/types/api'

const ADMIN_EMAIL = 'jowoosungtidal@gmail.com'

const formatDateTime = (value: string | null | undefined) => {
    if (!value) {
        return '-'
    }
    return new Intl.DateTimeFormat('ko-KR', {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
    }).format(new Date(value))
}

type PendingAction =
    | { kind: 'promote'; modelVersion: string }
    | { kind: 'disable'; modelVersion: string }
    | { kind: 'rollback' }

const SasrecModelAdminPage = () => {
    const { session } = useAuthSession()
    const [registry, setRegistry] = useState<SasrecRegistryAdminResponse | null>(null)
    const [loading, setLoading] = useState(false)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [notice, setNotice] = useState<string | null>(null)
    const [pendingAction, setPendingAction] = useState<PendingAction | null>(null)
    const [versionInput, setVersionInput] = useState('')

    const isAdmin = session?.email.toLowerCase() === ADMIN_EMAIL

    const load = useCallback(async (signal?: AbortSignal) => {
        if (!session || !isAdmin) {
            return
        }
        setLoading(true)
        setError(null)
        try {
            const response = await fetchLatestSasrecModelForAdmin(session.userId, signal)
            setRegistry(response)
        } catch (err) {
            if (signal?.aborted) {
                return
            }
            setError(err instanceof Error ? err.message : 'SASRec 모델 상태를 불러오지 못했습니다.')
        } finally {
            setLoading(false)
        }
    }, [isAdmin, session])

    useEffect(() => {
        const controller = new AbortController()
        void load(controller.signal)
        return () => controller.abort()
    }, [load])

    const runPending = async () => {
        if (!session || !pendingAction) {
            return
        }
        setBusy(true)
        setError(null)
        setNotice(null)
        try {
            let response: SasrecRegistryAdminResponse
            if (pendingAction.kind === 'promote') {
                response = await promoteSasrecModelForAdmin(session.userId, pendingAction.modelVersion)
                setNotice(`Promoted ${response.model_version ?? pendingAction.modelVersion}.`)
            } else if (pendingAction.kind === 'disable') {
                response = await disableSasrecModelForAdmin(session.userId, pendingAction.modelVersion)
                setNotice(`Disabled ${pendingAction.modelVersion}. Active model is now ${response.model_version ?? 'none'}.`)
            } else {
                response = await rollbackSasrecModelForAdmin(session.userId)
                setNotice(`Rolled back. Active model is now ${response.model_version ?? 'none'}.`)
            }
            setRegistry(response)
            setPendingAction(null)
            setVersionInput('')
        } catch (err) {
            setError(err instanceof Error ? err.message : 'SASRec registry 액션이 실패했습니다.')
        } finally {
            setBusy(false)
        }
    }

    const requestPromote = () => {
        if (!versionInput.trim()) {
            setError('promote 할 model_version을 입력하세요.')
            return
        }
        setPendingAction({ kind: 'promote', modelVersion: versionInput.trim() })
    }

    const requestDisable = () => {
        if (!versionInput.trim()) {
            setError('disable 할 model_version을 입력하세요.')
            return
        }
        setPendingAction({ kind: 'disable', modelVersion: versionInput.trim() })
    }

    const requestRollback = () => {
        setPendingAction({ kind: 'rollback' })
    }

    if (!session || !isAdmin) {
        return (
            <main className="space-y-6">
                <section className="rounded-2xl border border-hud-border-secondary bg-hud-bg-secondary/80 p-6">
                    <div className="flex items-center gap-3 text-amber-100">
                        <ShieldCheck size={22} />
                        <h2 className="text-xl font-semibold">SASRec Model Admin</h2>
                    </div>
                    <p className="mt-4 text-sm leading-6 text-hud-text-secondary">
                        이 화면은 {ADMIN_EMAIL} 관리자 계정에만 노출됩니다.
                    </p>
                </section>
            </main>
        )
    }

    const dialogDescription = () => {
        if (!pendingAction) {
            return undefined
        }
        if (pendingAction.kind === 'promote') {
            return `${pendingAction.modelVersion}을 active model로 promote 합니다.\n기존 promoted version은 rollback history로 이동합니다.`
        }
        if (pendingAction.kind === 'disable') {
            return `${pendingAction.modelVersion}을 disable 합니다.\nactive 였다면 직전 history 항목으로 자동 교체됩니다.`
        }
        return 'rollback history의 가장 최근 항목을 active로 되돌립니다.\nhistory가 비어 있으면 실패합니다.'
    }

    return (
        <main className="space-y-6">
            <section className="rounded-2xl border border-hud-border-secondary bg-hud-bg-secondary/85 p-6">
                <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                    <div>
                        <div className="flex items-center gap-3 text-hud-accent-primary">
                            <BadgeCheck size={24} />
                            <p className="text-xs font-semibold uppercase tracking-[0.26em]">SASRec Registry</p>
                        </div>
                        <h2 className="mt-3 text-2xl font-semibold text-hud-text-primary">
                            SASRec MVP 모델 운영
                        </h2>
                        <p className="mt-2 text-sm text-hud-text-secondary">
                            현재 active 모델을 확인하고 promote/disable/rollback 정책을 적용합니다.
                        </p>
                    </div>
                    <Button type="button" variant="outline" onClick={() => void load()} disabled={loading}>
                        <RefreshCw size={16} />
                        새로고침
                    </Button>
                </div>
                {notice && (
                    <div className="mt-5 flex items-start gap-3 rounded-xl border border-hud-accent-primary/30 bg-hud-accent-primary/10 p-4 text-sm text-hud-text-primary">
                        <BadgeCheck size={18} className="text-hud-accent-primary" />
                        <span>{notice}</span>
                    </div>
                )}
                {error && (
                    <div className="mt-5 flex items-start gap-3 rounded-xl border border-rose-300/30 bg-rose-500/10 p-4 text-sm text-rose-100">
                        <AlertTriangle size={18} />
                        <span>{error}</span>
                    </div>
                )}
            </section>

            <section className="grid gap-5 xl:grid-cols-[minmax(0,1.1fr)_minmax(0,0.9fr)]">
                <div className="rounded-2xl border border-hud-border-secondary bg-hud-bg-secondary/80 p-6">
                    <p className="text-xs uppercase tracking-[0.22em] text-hud-text-muted">Active Model</p>
                    <p className="mt-3 break-all text-lg font-semibold text-hud-text-primary">
                        {registry?.model_version ?? 'No active SASRec model.'}
                    </p>
                    <dl className="mt-5 grid grid-cols-2 gap-3 text-sm">
                        <Field label="Status" value={registry?.status ?? '-'} />
                        <Field label="User" value={registry?.user_id ?? '-'} />
                        <Field label="Generated" value={formatDateTime(registry?.generated_at_ai ?? null)} />
                        <Field label="Vocabulary" value={registry?.vocabulary_size?.toString() ?? '-'} />
                        <Field label="Train Examples" value={registry?.train_example_count?.toString() ?? '-'} />
                        <Field label="Artifact Dir" value={registry?.artifact_dir ?? '-'} multiline />
                    </dl>
                    {registry?.warnings && registry.warnings.length > 0 && (
                        <div className="mt-5 space-y-2 rounded-xl border border-amber-300/30 bg-amber-300/10 p-4 text-xs text-amber-100">
                            {registry.warnings.map((warning, index) => (
                                <p key={`${warning}-${index}`}>{warning}</p>
                            ))}
                        </div>
                    )}
                </div>

                <div className="rounded-2xl border border-hud-border-secondary bg-hud-bg-secondary/80 p-6">
                    <p className="text-xs uppercase tracking-[0.22em] text-hud-text-muted">Registry 액션</p>
                    <label className="mt-4 block text-sm text-hud-text-secondary" htmlFor="sasrec-model-version">
                        Model version
                    </label>
                    <input
                        id="sasrec-model-version"
                        type="text"
                        value={versionInput}
                        onChange={(e) => setVersionInput(e.target.value)}
                        placeholder="sasrec-mvp-..."
                        className="mt-2 w-full rounded-lg border border-hud-border-secondary bg-hud-bg-primary/60 px-3 py-2 text-sm text-hud-text-primary focus:border-hud-border-primary focus:outline-none"
                    />
                    <div className="mt-5 flex flex-wrap gap-2">
                        <Button type="button" variant="primary" onClick={requestPromote} disabled={busy}>
                            <BadgeCheck size={16} />
                            Promote
                        </Button>
                        <Button type="button" variant="outline" onClick={requestDisable} disabled={busy}>
                            <BoxSelect size={16} />
                            Disable
                        </Button>
                        <Button type="button" variant="outline" onClick={requestRollback} disabled={busy}>
                            <Undo2 size={16} />
                            Rollback
                        </Button>
                    </div>
                    <p className="mt-4 text-xs leading-6 text-hud-text-muted">
                        promote는 history에 직전 active를 push 합니다.<br />
                        disable은 disabled 목록에 추가하고 active 였다면 history에서 직전 항목으로 자동 교체합니다.<br />
                        rollback은 history pop으로 직전 active로 복원합니다.
                    </p>
                </div>
            </section>

            <ConfirmDialog
                open={pendingAction !== null}
                title={
                    pendingAction?.kind === 'promote'
                        ? 'SASRec 모델 promote'
                        : pendingAction?.kind === 'disable'
                            ? 'SASRec 모델 disable'
                            : 'SASRec 모델 rollback'
                }
                description={dialogDescription()}
                confirmLabel={pendingAction?.kind === 'rollback' ? 'Rollback' : pendingAction?.kind === 'disable' ? 'Disable' : 'Promote'}
                cancelLabel="취소"
                variant={pendingAction?.kind === 'promote' ? 'primary' : 'danger'}
                loading={busy}
                onConfirm={() => void runPending()}
                onCancel={() => {
                    if (!busy) {
                        setPendingAction(null)
                    }
                }}
            />

            <p className="text-xs text-hud-text-muted">
                <RotateCcw size={12} className="mr-1 inline" />
                latest 응답은 promote된 model이 있으면 그것을, 없으면 disabled를 제외한 시간순 정렬을 사용합니다.
            </p>
        </main>
    )
}

const Field = ({ label, value, multiline }: { label: string; value: string; multiline?: boolean }) => (
    <div className="rounded-xl border border-hud-border-secondary bg-hud-bg-primary/60 p-3">
        <p className="text-[10px] uppercase tracking-[0.22em] text-hud-text-muted">{label}</p>
        <p className={`mt-1 text-sm text-hud-text-primary ${multiline ? 'break-all' : 'truncate'}`}>{value}</p>
    </div>
)

export default SasrecModelAdminPage
