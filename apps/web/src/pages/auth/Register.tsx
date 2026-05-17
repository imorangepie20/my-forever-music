import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
    ArrowRight,
    CheckCircle2,
    Eye,
    EyeOff,
    Loader,
    Lock,
    Mail,
    Radio,
    ShieldCheck,
    User,
} from 'lucide-react'
import Button from '../../components/common/Button'
import { useAuthSession } from '../../contexts/AuthSessionContext'
import { useRecommendationWorkspace } from '../../contexts/RecommendationWorkspaceContext'
import { ApiError, fetchPlatformCatalog, registerAccount } from '../../services/api'
import type { AuthRegistrationResponse, PlatformCatalogResponse, WorkspacePlatformId } from '../../types/api'

const Register = () => {
    const { setSessionFromAuthentication } = useAuthSession()
    const { resetWorkspace, updateWorkspace } = useRecommendationWorkspace()
    const [showPassword, setShowPassword] = useState(false)
    const [name, setName] = useState('')
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [confirmPassword, setConfirmPassword] = useState('')
    const [preferredPlatformId, setPreferredPlatformId] = useState<WorkspacePlatformId>('spotify')
    const [marketingOptIn, setMarketingOptIn] = useState(false)
    const [acceptedTerms, setAcceptedTerms] = useState(false)
    const [acceptedPrivacyPolicy, setAcceptedPrivacyPolicy] = useState(false)
    const [platforms, setPlatforms] = useState<PlatformCatalogResponse['platforms']>([])
    const [loadingPlatforms, setLoadingPlatforms] = useState(true)
    const [submitting, setSubmitting] = useState(false)
    const [errorMessage, setErrorMessage] = useState<string | null>(null)
    const [successState, setSuccessState] = useState<AuthRegistrationResponse | null>(null)
    const pmsImportPlatforms = platforms.filter((platform) => platform.pms_import_supported)

    useEffect(() => {
        const controller = new AbortController()

        fetchPlatformCatalog(controller.signal)
            .then((response) => {
                setPlatforms(response.platforms)
                const primaryStreamingPlatforms = response.platforms.filter((platform) => platform.pms_import_supported)
                if (primaryStreamingPlatforms.length > 0) {
                    setPreferredPlatformId(primaryStreamingPlatforms[0].platform_id)
                }
                setLoadingPlatforms(false)
            })
            .catch(() => {
                setLoadingPlatforms(false)
            })

        return () => controller.abort()
    }, [])

    const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
        event.preventDefault()

        if (password !== confirmPassword) {
            setErrorMessage('Password confirmation does not match.')
            return
        }

        setSubmitting(true)
        setErrorMessage(null)

        try {
            const response = await registerAccount({
                display_name: name.trim(),
                email: email.trim(),
                password,
                preferred_platform_id: preferredPlatformId,
                marketing_opt_in: marketingOptIn,
                accepted_terms: acceptedTerms,
                accepted_privacy_policy: acceptedPrivacyPolicy,
            })

            setSessionFromAuthentication(response)
            resetWorkspace()
            updateWorkspace({
                userId: response.user.user_id,
                preferredPlatformId: response.onboarding.preferred_platform_id,
            })
            setSuccessState(response)
        } catch (error: unknown) {
            if (error instanceof ApiError) {
                setErrorMessage(error.message)
            } else {
                setErrorMessage('Unable to create your account right now.')
            }
        } finally {
            setSubmitting(false)
        }
    }

    return (
        <div className="min-h-screen bg-hud-bg-primary hud-grid-bg px-6 py-10">
            <div className="mx-auto grid w-full max-w-6xl gap-8 xl:grid-cols-[1.1fr_0.9fr]">
                <section className="rounded-[28px] border border-hud-border-secondary bg-hud-bg-secondary/80 p-8 backdrop-blur-xl xl:p-10">
                    <div className="max-w-2xl">
                        <div className="inline-flex items-center gap-3 rounded-full border border-hud-border-primary bg-hud-accent-primary/10 px-4 py-2 text-xs font-semibold uppercase tracking-[0.28em] text-hud-accent-primary">
                            <ShieldCheck size={15} />
                            Music Home Onboarding
                        </div>

                        <h1 className="mt-6 text-4xl font-semibold tracking-tight text-hud-text-primary sm:text-5xl">
                            Keep your playlists and taste library even when your streaming platform changes.
                        </h1>
                        <p className="mt-5 max-w-xl text-base leading-7 text-hud-text-secondary">
                            Create your account, choose the subscription platform that currently holds your playlists,
                            and then connect it so PMS can preserve your music library before recommendations begin.
                        </p>

                        <div className="mt-8 grid gap-4 sm:grid-cols-3">
                            {[
                                {
                                    title: 'Own Your Library',
                                    body: 'Your imported playlists become a PMS library that is not tied to one platform account.',
                                },
                                {
                                    title: 'Connect One Source',
                                    body: 'Start from the streaming service you already subscribe to and use most often.',
                                },
                                {
                                    title: 'Train Your Taste',
                                    body: 'After import, your tracks and any resolved audio features become the first input for your music model.',
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

                        <div className="mt-8 rounded-3xl border border-hud-border-secondary bg-hud-bg-primary/70 p-5">
                            <div className="flex items-center gap-3">
                                <span className="rounded-xl bg-hud-accent-primary/10 p-2 text-hud-accent-primary">
                                    <Radio size={18} />
                                </span>
                                <div>
                                    <p className="text-xs uppercase tracking-[0.24em] text-hud-text-muted">
                                        Primary Streaming Sources
                                    </p>
                                    <p className="mt-1 text-sm text-hud-text-primary">
                                        {loadingPlatforms
                                            ? 'Loading platform catalog...'
                                            : pmsImportPlatforms.map((platform) => platform.display_name).join(' / ')
                                                || 'Spotify'}
                                    </p>
                                    <p className="mt-2 text-xs leading-5 text-hud-text-muted">
                                        We will open streaming imports in this order: Spotify first, then TIDAL, then YouTube Music. Apple Music stays deferred until the developer account is ready, and Last.fm is a listening-history signal rather than the primary playlist source.
                                    </p>
                                </div>
                            </div>
                        </div>
                    </div>
                </section>

                <section className="rounded-[28px] border border-hud-border-secondary bg-hud-bg-secondary/88 p-8 backdrop-blur-xl xl:p-10">
                    <div className="flex items-center justify-between gap-4">
                        <div>
                            <p className="text-xs font-semibold uppercase tracking-[0.26em] text-hud-accent-primary">
                                Create Account
                            </p>
                            <h2 className="mt-3 text-2xl font-semibold text-hud-text-primary">
                                Start your music home
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
                                            Registration Complete
                                        </p>
                                        <h3 className="mt-2 text-xl font-semibold text-hud-text-primary">
                                            {successState.user.display_name}'s music home is ready to connect.
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
                                    <p className="text-xs uppercase tracking-[0.22em] text-hud-text-muted">Preferred Platform</p>
                                    <p className="mt-2 text-sm text-hud-text-primary">
                                        {successState.onboarding.preferred_platform_id}
                                    </p>
                                </div>
                            </div>

                            <div className="flex flex-wrap gap-3">
                                <Link to={successState.onboarding.next_step_path}>
                                    <Button variant="primary" glow rightIcon={<ArrowRight size={16} />}>
                                        Continue to Platforms
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
                                <label className="mb-2 block text-sm text-hud-text-secondary">Display Name</label>
                                <div className="relative">
                                    <User className="absolute left-4 top-1/2 -translate-y-1/2 text-hud-text-muted" size={18} />
                                    <input
                                        type="text"
                                        value={name}
                                        onChange={(e) => setName(e.target.value)}
                                        placeholder="Forever Listener"
                                        className="w-full rounded-xl border border-hud-border-secondary bg-hud-bg-primary pl-12 pr-4 py-3 text-hud-text-primary placeholder-hud-text-muted focus:border-hud-accent-primary focus:outline-none transition-hud"
                                    />
                                </div>
                            </div>

                            <div>
                                <label className="mb-2 block text-sm text-hud-text-secondary">Email</label>
                                <div className="relative">
                                    <Mail className="absolute left-4 top-1/2 -translate-y-1/2 text-hud-text-muted" size={18} />
                                    <input
                                        type="email"
                                        value={email}
                                        onChange={(e) => setEmail(e.target.value)}
                                        placeholder="listener@example.com"
                                        className="w-full rounded-xl border border-hud-border-secondary bg-hud-bg-primary pl-12 pr-4 py-3 text-hud-text-primary placeholder-hud-text-muted focus:border-hud-accent-primary focus:outline-none transition-hud"
                                    />
                                </div>
                            </div>

                            <div className="grid gap-5 sm:grid-cols-2">
                                <div>
                                    <label className="mb-2 block text-sm text-hud-text-secondary">Password</label>
                                    <div className="relative">
                                        <Lock className="absolute left-4 top-1/2 -translate-y-1/2 text-hud-text-muted" size={18} />
                                        <input
                                            type={showPassword ? 'text' : 'password'}
                                            value={password}
                                            onChange={(e) => setPassword(e.target.value)}
                                            placeholder="At least 8 chars"
                                            className="w-full rounded-xl border border-hud-border-secondary bg-hud-bg-primary pl-12 pr-12 py-3 text-hud-text-primary placeholder-hud-text-muted focus:border-hud-accent-primary focus:outline-none transition-hud"
                                        />
                                        <button
                                            type="button"
                                            onClick={() => setShowPassword((current) => !current)}
                                            className="absolute right-4 top-1/2 -translate-y-1/2 text-hud-text-muted transition-hud hover:text-hud-text-primary"
                                        >
                                            {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                                        </button>
                                    </div>
                                </div>

                                <div>
                                    <label className="mb-2 block text-sm text-hud-text-secondary">Confirm Password</label>
                                    <div className="relative">
                                        <Lock className="absolute left-4 top-1/2 -translate-y-1/2 text-hud-text-muted" size={18} />
                                        <input
                                            type={showPassword ? 'text' : 'password'}
                                            value={confirmPassword}
                                            onChange={(e) => setConfirmPassword(e.target.value)}
                                            placeholder="Repeat password"
                                            className="w-full rounded-xl border border-hud-border-secondary bg-hud-bg-primary pl-12 pr-4 py-3 text-hud-text-primary placeholder-hud-text-muted focus:border-hud-accent-primary focus:outline-none transition-hud"
                                        />
                                    </div>
                                </div>
                            </div>

                            <div>
                                <label className="mb-2 block text-sm text-hud-text-secondary">Preferred Streaming Platform</label>
                                <select
                                    value={preferredPlatformId}
                                    onChange={(e) => setPreferredPlatformId(e.target.value as typeof preferredPlatformId)}
                                    className="w-full rounded-xl border border-hud-border-secondary bg-hud-bg-primary px-4 py-3 text-hud-text-primary focus:border-hud-accent-primary focus:outline-none transition-hud"
                                >
                                    {pmsImportPlatforms.length > 0 ? (
                                        pmsImportPlatforms.map((platform) => (
                                            <option key={platform.platform_id} value={platform.platform_id}>
                                                {platform.display_name}
                                            </option>
                                        ))
                                    ) : (
                                        <>
                                            <option value="spotify">Spotify</option>
                                        </>
                                    )}
                                </select>
                                <p className="mt-2 text-xs leading-5 text-hud-text-muted">
                                    Choose the service that should feed your first PMS playlist import. Last.fm can be
                                    connected after signup to enrich your listening model.
                                </p>
                            </div>

                            <label className="flex items-start gap-3 rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                                <input
                                    type="checkbox"
                                    checked={marketingOptIn}
                                    onChange={(e) => setMarketingOptIn(e.target.checked)}
                                    className="mt-1 h-4 w-4 rounded border-hud-border-secondary bg-hud-bg-primary text-hud-accent-primary focus:ring-hud-accent-primary"
                                />
                                <span className="text-sm leading-6 text-hud-text-secondary">
                                    Send occasional updates about new recommendation features and platform integrations.
                                </span>
                            </label>

                            <label className="flex items-start gap-3 rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                                <input
                                    type="checkbox"
                                    checked={acceptedTerms}
                                    onChange={(e) => setAcceptedTerms(e.target.checked)}
                                    className="mt-1 h-4 w-4 rounded border-hud-border-secondary bg-hud-bg-primary text-hud-accent-primary focus:ring-hud-accent-primary"
                                />
                                <span className="text-sm leading-6 text-hud-text-secondary">
                                    I agree to the Terms of Service for account creation and platform onboarding.
                                </span>
                            </label>

                            <label className="flex items-start gap-3 rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                                <input
                                    type="checkbox"
                                    checked={acceptedPrivacyPolicy}
                                    onChange={(e) => setAcceptedPrivacyPolicy(e.target.checked)}
                                    className="mt-1 h-4 w-4 rounded border-hud-border-secondary bg-hud-bg-primary text-hud-accent-primary focus:ring-hud-accent-primary"
                                />
                                <span className="text-sm leading-6 text-hud-text-secondary">
                                    I agree to the Privacy Policy for storing profile, playlist import, audio feature
                                    snapshot, recommendation, and listening behavior data.
                                </span>
                            </label>

                            {errorMessage && (
                                <div className="rounded-2xl border border-rose-400/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">
                                    {errorMessage}
                                </div>
                            )}

                            <Button
                                variant="primary"
                                fullWidth
                                glow
                                type="submit"
                                disabled={submitting}
                                rightIcon={submitting ? <Loader className="animate-spin" size={16} /> : <ArrowRight size={16} />}
                            >
                                {submitting ? 'Creating account...' : 'Create account and continue'}
                            </Button>
                        </form>
                    )}
                </section>
            </div>
        </div>
    )
}

export default Register
