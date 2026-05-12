import { useEffect } from 'react'
import { AlertTriangle } from 'lucide-react'
import Button from './Button'

interface ConfirmDialogProps {
    open: boolean
    title: string
    description?: string
    confirmLabel?: string
    cancelLabel?: string
    variant?: 'danger' | 'primary'
    loading?: boolean
    onConfirm: () => void
    onCancel: () => void
}

const ConfirmDialog = ({
    open,
    title,
    description,
    confirmLabel = '확인',
    cancelLabel = '취소',
    variant = 'danger',
    loading = false,
    onConfirm,
    onCancel,
}: ConfirmDialogProps) => {
    useEffect(() => {
        if (!open) {
            return
        }
        const onKey = (event: KeyboardEvent) => {
            if (event.key === 'Escape' && !loading) {
                onCancel()
            }
        }
        document.addEventListener('keydown', onKey)
        return () => document.removeEventListener('keydown', onKey)
    }, [open, loading, onCancel])

    if (!open) {
        return null
    }

    const iconWrapClass = variant === 'danger'
        ? 'bg-hud-accent-danger/10'
        : 'bg-hud-accent-primary/10'
    const iconColorClass = variant === 'danger'
        ? 'text-hud-accent-danger'
        : 'text-hud-accent-primary'

    return (
        <div
            className="fixed inset-0 z-50 flex items-center justify-center p-4"
            role="dialog"
            aria-modal="true"
            aria-labelledby="confirm-dialog-title"
        >
            <div
                className="absolute inset-0 bg-black/60 backdrop-blur-sm"
                onClick={loading ? undefined : onCancel}
            />
            <div className="relative hud-card hud-card-bottom rounded-lg w-full max-w-sm animate-fade-in">
                <div className="p-6 text-center">
                    <div className={`w-16 h-16 mx-auto mb-4 rounded-full flex items-center justify-center ${iconWrapClass}`}>
                        <AlertTriangle size={32} className={iconColorClass} />
                    </div>
                    <h3 id="confirm-dialog-title" className="text-lg font-semibold text-hud-text-primary mb-2">
                        {title}
                    </h3>
                    {description && (
                        <p className="text-sm text-hud-text-secondary whitespace-pre-line">
                            {description}
                        </p>
                    )}
                </div>
                <div className="flex gap-3 p-5 border-t border-hud-border-secondary">
                    <Button
                        type="button"
                        variant="ghost"
                        fullWidth
                        disabled={loading}
                        onClick={onCancel}
                    >
                        {cancelLabel}
                    </Button>
                    <Button
                        type="button"
                        variant={variant}
                        fullWidth
                        disabled={loading}
                        onClick={onConfirm}
                    >
                        {confirmLabel}
                    </Button>
                </div>
            </div>
        </div>
    )
}

export default ConfirmDialog
