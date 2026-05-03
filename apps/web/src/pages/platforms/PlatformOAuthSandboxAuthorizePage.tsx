import { ExternalLink, ShieldCheck, XCircle } from 'lucide-react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import Button from '@/components/common/Button'
import HudCard from '@/components/common/HudCard'
import type { PlatformAuthorizationStartResponse } from '@/types/api'

const STORAGE_KEY = 'my-forever-music.platform-oauth-session'

const loadPendingAuthorization = (state: string | null): PlatformAuthorizationStartResponse | null => {
    if (!state || typeof window === 'undefined') {
        return null
    }

    const stored = window.sessionStorage.getItem(`${STORAGE_KEY}.${state}`)
    if (!stored) {
        return null
    }

    try {
        return JSON.parse(stored) as PlatformAuthorizationStartResponse
    } catch {
        return null
    }
}

const PlatformOAuthSandboxAuthorizePage = () => {
    const [params] = useSearchParams()
    const navigate = useNavigate()
    const state = params.get('state')
    const pending = loadPendingAuthorization(state)

    const handleCancel = () => {
        if (state && typeof window !== 'undefined') {
            window.sessionStorage.removeItem(`${STORAGE_KEY}.${state}`)
        }

        navigate('/platforms')
    }

    if (!pending || !state) {
        return (
            <div className="space-y-6">
                <HudCard title="Sandbox OAuth Session Missing" subtitle="No pending authorization was found for this state">
                    <p className="text-sm leading-6 text-hud-text-secondary">
                        Start the platform connection again from the platforms page. The temporary sandbox authorization
                        payload may have expired or been cleared from this browser session.
                    </p>
                    <div className="mt-5">
                        <Link to="/platforms">
                            <Button variant="primary">Back to Platforms</Button>
                        </Link>
                    </div>
                </HudCard>
            </div>
        )
    }

    if (pending.authorization.authorization_channel !== 'internal_approval_page') {
        return (
            <div className="space-y-6">
                <HudCard title="External OAuth Redirect" subtitle="This authorization session should continue in the provider browser flow">
                    <p className="text-sm leading-6 text-hud-text-secondary">
                        This platform is configured to use an external OAuth redirect instead of the internal sandbox
                        approval screen.
                    </p>
                    <div className="mt-5 flex gap-3">
                        <Link to="/platforms">
                            <Button variant="outline">Back to Platforms</Button>
                        </Link>
                        {pending.authorization.external_authorization_url ? (
                            <a href={pending.authorization.external_authorization_url}>
                                <Button variant="primary" glow>
                                    Open Provider Authorization
                                </Button>
                            </a>
                        ) : null}
                    </div>
                </HudCard>
            </div>
        )
    }

    return (
        <div className="space-y-6">
            <HudCard className="overflow-hidden">
                <div className="relative">
                    <div className="absolute inset-x-0 top-0 h-40 rounded-3xl bg-gradient-to-r from-hud-accent-primary/20 via-cyan-300/10 to-hud-accent-secondary/15 blur-3xl" />
                    <div className="relative">
                        <p className="text-xs font-semibold uppercase tracking-[0.28em] text-hud-accent-primary">
                            Sandbox OAuth Approval
                        </p>
                        <h2 className="mt-4 text-3xl font-semibold tracking-tight text-hud-text-primary sm:text-4xl">
                            Approve {pending.authorization.platform_display_name} for {pending.user.display_name}
                        </h2>
                        <p className="mt-4 max-w-2xl text-base leading-7 text-hud-text-secondary">
                            This screen simulates the provider consent step. In the real implementation this route will
                            be replaced by the external platform OAuth page and callback.
                        </p>
                    </div>
                </div>
            </HudCard>

            <div className="grid gap-6 xl:grid-cols-[1fr_0.9fr]">
                <HudCard title="Requested Access" subtitle="Scopes requested for the first PMS import">
                    <div className="space-y-4">
                        {pending.authorization.requested_scopes.map((scope) => (
                            <div
                                key={scope}
                                className="flex items-center gap-3 rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4"
                            >
                                <span className="rounded-xl bg-hud-accent-primary/10 p-2 text-hud-accent-primary">
                                    <ShieldCheck size={18} />
                                </span>
                                <div>
                                    <p className="text-sm font-medium text-hud-text-primary">{scope}</p>
                                    <p className="mt-1 text-sm text-hud-text-secondary">
                                        Needed to read profile basics and the playlist catalog for PMS onboarding.
                                    </p>
                                </div>
                            </div>
                        ))}
                    </div>
                </HudCard>

                <HudCard title="Authorization Summary" subtitle="Temporary session issued by services/api">
                    <div className="space-y-4">
                        <div className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                            <p className="text-xs uppercase tracking-[0.22em] text-hud-text-muted">State</p>
                            <p className="mt-2 break-all text-sm text-hud-text-primary">{pending.authorization.state}</p>
                        </div>
                        <div className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                            <p className="text-xs uppercase tracking-[0.22em] text-hud-text-muted">Expires At</p>
                            <p className="mt-2 text-sm text-hud-text-primary">
                                {new Date(pending.authorization.expires_at).toLocaleString()}
                            </p>
                        </div>
                        <div className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                            <p className="text-xs uppercase tracking-[0.22em] text-hud-text-muted">Mode</p>
                            <p className="mt-2 text-sm text-hud-text-primary">{pending.authorization.authorization_mode}</p>
                        </div>

                        <div className="flex flex-wrap gap-3">
                            <Link to={pending.authorization.callback_path}>
                                <Button variant="primary" glow rightIcon={<ExternalLink size={16} />}>
                                    Approve and Continue
                                </Button>
                            </Link>
                            <Button variant="outline" onClick={handleCancel} leftIcon={<XCircle size={16} />}>
                                Cancel
                            </Button>
                        </div>
                    </div>
                </HudCard>
            </div>
        </div>
    )
}

export default PlatformOAuthSandboxAuthorizePage
