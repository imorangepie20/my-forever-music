import { useState } from 'react'
import { Link } from 'react-router-dom'
import {
    ArrowRight,
    CheckCircle2,
    Eye,
    EyeOff,
    Lock,
    Mail,
    ShieldCheck,
} from 'lucide-react'
import Button from '../../components/common/Button'
import { useAuthSession } from '../../contexts/AuthSessionContext'
import { useRecommendationWorkspace } from '../../contexts/RecommendationWorkspaceContext'
import { ApiError, loginAccount } from '../../services/api'
import type { AuthLoginResponse } from '../../types/api'

const Login = () => {
    const { setSessionFromAuthentication } = useAuthSession()
    const { updateWorkspace } = useRecommendationWorkspace()
    const [showPassword, setShowPassword] = useState(false)
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [submitting, setSubmitting] = useState(false)
    const [errorMessage, setErrorMessage] = useState<string | null>(null)
    const [successState, setSuccessState] = useState<AuthLoginResponse | null>(null)

    const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
        event.preventDefault()

        setSubmitting(true)
        setErrorMessage(null)

        try {
            const response = await loginAccount({
                email: email.trim(),
                password,
            })

            setSessionFromAuthentication(response)
            updateWorkspace({
                userId: response.user.user_id,
                preferredPlatformId: response.onboarding.preferred_platform_id,
            })
            setSuccessState(response)
        } catch (error: unknown) {
            if (error instanceof ApiError) {
                setErrorMessage(error.message)
            } else {
                setErrorMessage('Unable to sign in right now.')
            }
        } finally {
            setSubmitting(false)
        }
    }

    return (
        <div className="min-h-screen bg-hud-bg-primary hud-grid-bg px-6 py-10">
            <div className="mx-auto grid w-full max-w-5xl gap-8 xl:grid-cols-[1.05fr_0.95fr]">
                <section className="rounded-[28px] border border-hud-border-secondary bg-hud-bg-secondary/80 p-8 backdrop-blur-xl xl:p-10">
                    <div className="max-w-2xl">
                        <div className="inline-flex items-center gap-3 rounded-full border border-hud-border-primary bg-hud-accent-primary/10 px-4 py-2 text-xs font-semibold uppercase tracking-[0.28em] text-hud-accent-primary">
                            <ShieldCheck size={15} />
                            Session Restore
                        </div>

                        <h1 className="mt-6 text-4xl font-semibold tracking-tight text-hud-text-primary sm:text-5xl">
                            Sign back in and continue the exact onboarding step you left off at.
                        </h1>
                        <p className="mt-5 max-w-xl text-base leading-7 text-hud-text-secondary">
                            This login flow is primarily for local-first service testing on the MacBook. It restores the
                            saved account shell and the current onboarding destination, so we can keep iterating without
                            re-registering every time.
                        </p>

                        <div className="mt-8 grid gap-4 sm:grid-cols-3">
                            {[
                                {
                                    title: 'Restore Session',
                                    body: 'Recover the current account context from email and password.',
                                },
                                {
                                    title: 'Resume Onboarding',
                                    body: 'Return to Platforms or PMS based on the saved platform connection state.',
                                },
                                {
                                    title: 'Keep Testing',
                                    body: 'Stay in the MacBook local loop while the service flow is still evolving.',
                                },
                            ].map((item) => (
                                <div
                                    key={item.title}
                                    className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-5"
                                >
                                    <p className="text-sm font-semibold text-hud-text-primary">{item.title}</p>
                                    <p className="mt-2 text-sm leading-6 text-hud-text-secondary">{item.body}</p>
                                </div>
                            ))}
                        </div>
                    </div>
                </section>

                <section className="rounded-[28px] border border-hud-border-secondary bg-hud-bg-secondary/88 p-8 backdrop-blur-xl xl:p-10">
                    <div className="flex items-center justify-between gap-4">
                        <div>
                            <p className="text-xs font-semibold uppercase tracking-[0.26em] text-hud-accent-primary">
                                Sign In
                            </p>
                            <h2 className="mt-3 text-2xl font-semibold text-hud-text-primary">
                                Continue local testing
                            </h2>
                        </div>
                        <Link
                            to="/"
                            className="text-sm text-hud-text-muted transition-hud hover:text-hud-text-primary"
                        >
                            Back to workspace
                        </Link>
                    </div>

                    {successState ? (
                        <div className="mt-8 space-y-6">
                            <div className="rounded-3xl border border-emerald-400/30 bg-emerald-400/10 p-6">
                                <div className="flex items-start gap-4">
                                    <span className="rounded-2xl bg-emerald-400/15 p-3 text-emerald-300">
                                        <CheckCircle2 size={22} />
                                    </span>
                                    <div>
                                        <p className="text-sm font-semibold uppercase tracking-[0.22em] text-emerald-300">
                                            Authentication Complete
                                        </p>
                                        <h3 className="mt-2 text-xl font-semibold text-hud-text-primary">
                                            {successState.user.display_name} is back in the control loop.
                                        </h3>
                                        <p className="mt-3 text-sm leading-6 text-hud-text-secondary">
                                            {successState.onboarding.next_step_message}
                                        </p>
                                    </div>
                                </div>
                            </div>

                            <div className="grid gap-4 sm:grid-cols-2">
                                <div className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-5">
                                    <p className="text-xs uppercase tracking-[0.22em] text-hud-text-muted">User ID</p>
                                    <p className="mt-2 text-sm text-hud-text-primary">{successState.user.user_id}</p>
                                </div>
                                <div className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-5">
                                    <p className="text-xs uppercase tracking-[0.22em] text-hud-text-muted">Next Step</p>
                                    <p className="mt-2 text-sm text-hud-text-primary">
                                        {successState.onboarding.next_step_path}
                                    </p>
                                </div>
                            </div>

                            <div className="flex flex-wrap gap-3">
                                <Link to={successState.onboarding.next_step_path}>
                                    <Button variant="primary" glow rightIcon={<ArrowRight size={16} />}>
                                        Continue
                                    </Button>
                                </Link>
                                <Link to="/">
                                    <Button variant="outline">Open Control Room</Button>
                                </Link>
                            </div>
                        </div>
                    ) : (
                        <form onSubmit={handleSubmit} className="mt-8 space-y-5">
                            <div>
                                <label className="mb-2 block text-sm text-hud-text-secondary">Email</label>
                                <div className="relative">
                                    <Mail className="absolute left-4 top-1/2 -translate-y-1/2 text-hud-text-muted" size={18} />
                                    <input
                                        type="email"
                                        value={email}
                                        onChange={(event) => setEmail(event.target.value)}
                                        placeholder="listener@example.com"
                                        className="w-full rounded-xl border border-hud-border-secondary bg-hud-bg-primary pl-12 pr-4 py-3 text-hud-text-primary placeholder-hud-text-muted focus:border-hud-accent-primary focus:outline-none transition-hud"
                                    />
                                </div>
                            </div>

                            <div>
                                <label className="mb-2 block text-sm text-hud-text-secondary">Password</label>
                                <div className="relative">
                                    <Lock className="absolute left-4 top-1/2 -translate-y-1/2 text-hud-text-muted" size={18} />
                                    <input
                                        type={showPassword ? 'text' : 'password'}
                                        value={password}
                                        onChange={(event) => setPassword(event.target.value)}
                                        placeholder="music2026"
                                        className="w-full rounded-xl border border-hud-border-secondary bg-hud-bg-primary pl-12 pr-12 py-3 text-hud-text-primary placeholder-hud-text-muted focus:border-hud-accent-primary focus:outline-none transition-hud"
                                    />
                                    <button
                                        type="button"
                                        onClick={() => setShowPassword((current) => !current)}
                                        className="absolute right-4 top-1/2 -translate-y-1/2 text-hud-text-muted hover:text-hud-text-primary transition-hud"
                                    >
                                        {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                                    </button>
                                </div>
                            </div>

                            {errorMessage && (
                                <div className="rounded-2xl border border-hud-accent-danger/40 bg-hud-accent-danger/10 p-4 text-sm leading-6 text-hud-text-secondary">
                                    {errorMessage}
                                </div>
                            )}

                            <Button variant="primary" fullWidth glow type="submit" disabled={submitting}>
                                {submitting ? 'Signing In...' : 'Sign In'}
                            </Button>

                            <p className="text-center text-sm text-hud-text-muted">
                                Need a fresh account?{' '}
                                <Link to="/signup" className="text-hud-accent-primary hover:underline">
                                    Create one here
                                </Link>
                            </p>
                        </form>
                    )}
                </section>
            </div>
        </div>
    )
}

export default Login
