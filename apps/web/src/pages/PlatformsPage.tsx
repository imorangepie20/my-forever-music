import { useEffect, useState } from 'react'
import { ArrowRight, CheckCircle2, Disc3, Radio, RefreshCw, Sparkles } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import Button from '@/components/common/Button'
import HudCard from '@/components/common/HudCard'
import { useAuthSession } from '@/contexts/AuthSessionContext'
import { useRecommendationWorkspace } from '@/contexts/RecommendationWorkspaceContext'
import {
    ApiError,
    disconnectPlatformAccount,
    fetchPlatformCatalog,
    fetchPlatformConnectionBootstrap,
    startPlatformAuthorization,
} from '@/services/api'
import type {
    PlatformCatalogResponse,
    PlatformConnectionBootstrapResponse,
    WorkspacePlatformId,
} from '@/types/api'

const stageLabel: Record<string, string> = {
    'planned-pms-import': 'Planned PMS Import',
    'priority-analysis-source': 'Priority Analysis Source',
}

const OAUTH_STORAGE_KEY = 'my-forever-music.platform-oauth-session'

const PlatformsPage = () => {
    const navigate = useNavigate()
    const { session, updateSession } = useAuthSession()
    const { workspace, updateWorkspace } = useRecommendationWorkspace()
    const [catalog, setCatalog] = useState<PlatformCatalogResponse | null>(null)
    const [connectionBootstrap, setConnectionBootstrap] = useState<PlatformConnectionBootstrapResponse | null>(null)
    const [isLoading, setIsLoading] = useState(true)
    const [isMutating, setIsMutating] = useState<string | null>(null)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        const controller = new AbortController()

        setIsLoading(true)
        setError(null)

        fetchPlatformCatalog(controller.signal)
            .then((response) => {
                setCatalog(response)
                if (!session) {
                    setConnectionBootstrap(null)
                    return null
                }

                return fetchPlatformConnectionBootstrap(session.userId, controller.signal)
            })
            .then((bootstrap) => {
                if (bootstrap) {
                    setConnectionBootstrap(bootstrap)
                    updateWorkspace({
                        userId: bootstrap.user.user_id,
                        preferredPlatformId: bootstrap.user.preferred_platform_id,
                    })
                }
                setError(null)
            })
            .catch((requestError: unknown) => {
                if (requestError instanceof DOMException && requestError.name === 'AbortError') {
                    return
                }

                const message =
                    requestError instanceof ApiError
                        ? requestError.message
                        : 'Unable to load the platform catalog from the Spring Boot API.'

                setCatalog(null)
                setError(message)
            })
            .finally(() => {
                setIsLoading(false)
            })

        return () => controller.abort()
    }, [session, updateWorkspace])

    const reloadConnections = async () => {
        if (!session) {
            return
        }

        const bootstrap = await fetchPlatformConnectionBootstrap(session.userId)
        setConnectionBootstrap(bootstrap)
        updateSession({
            preferredPlatformId: bootstrap.user.preferred_platform_id,
            onboardingStage: bootstrap.summary.onboarding_stage,
            nextStepPath: bootstrap.summary.next_step_path,
            nextStepMessage: bootstrap.summary.next_step_message,
            platformConnectionRequired: !bootstrap.summary.preferred_platform_connected,
        })
    }

    const handleConnectToggle = async (platformId: WorkspacePlatformId, connected: boolean) => {
        if (!session) {
            setError('Create an account first so platform onboarding can attach to a user.')
            return
        }

        setIsMutating(platformId)
        setError(null)

        try {
            if (connected) {
                await disconnectPlatformAccount({
                    user_id: session.userId,
                    platform_id: platformId,
                })

                await reloadConnections()
            } else {
                const response = await startPlatformAuthorization({
                    user_id: session.userId,
                    platform_id: platformId,
                })

                if (typeof window !== 'undefined') {
                    window.sessionStorage.setItem(
                        `${OAUTH_STORAGE_KEY}.${response.authorization.state}`,
                        JSON.stringify(response),
                    )
                }

                if (
                    response.authorization.authorization_channel === 'external_browser_redirect' &&
                    response.authorization.external_authorization_url
                ) {
                    window.location.assign(response.authorization.external_authorization_url)
                    return
                }

                if (!response.authorization.approval_page_path) {
                    throw new Error('Authorization approval page is missing for the current platform mode.')
                }

                navigate(response.authorization.approval_page_path)
                return
            }
        } catch (requestError: unknown) {
            const message =
                requestError instanceof ApiError
                    ? requestError.message
                    : 'Unable to update platform connection state right now.'
            setError(message)
        } finally {
            setIsMutating(null)
        }
    }

    return (
        <div className="space-y-6">
            <section className="grid gap-6 xl:grid-cols-[1.15fr_0.85fr]">
                <HudCard className="overflow-hidden">
                    <div className="relative">
                        <div className="absolute inset-x-0 top-0 h-44 rounded-3xl bg-gradient-to-r from-hud-accent-primary/20 via-cyan-300/10 to-hud-accent-secondary/15 blur-3xl" />
                        <div className="relative">
                            <p className="text-xs font-semibold uppercase tracking-[0.28em] text-hud-accent-primary">
                                Platform Intake
                            </p>
                            <h2 className="mt-4 max-w-3xl text-3xl font-semibold tracking-tight text-hud-text-primary sm:text-4xl">
                                Pick the streaming platform that should feed PMS first.
                            </h2>
                            <p className="mt-4 max-w-2xl text-base leading-7 text-hud-text-secondary">
                                This route is the first implementation slice of the onboarding story from
                                `PROJECT_KEY_SERVICE.md`: choose a subscribed platform, define how audio features
                                will be resolved, and prepare the PMS import path.
                            </p>

                            <div className="mt-8 flex flex-wrap gap-3">
                                {connectionBootstrap?.summary.next_step_path ? (
                                    <Link to={connectionBootstrap.summary.next_step_path}>
                                        <Button type="button" variant="primary" glow>
                                            {connectionBootstrap.summary.preferred_platform_connected
                                                ? 'Continue to PMS'
                                                : 'Stay in Platform Onboarding'}
                                            <ArrowRight size={16} />
                                        </Button>
                                    </Link>
                                ) : (
                                    <Link to="/signup">
                                        <Button type="button" variant="primary" glow>
                                            Start Signup
                                            <ArrowRight size={16} />
                                        </Button>
                                    </Link>
                                )}
                                <Link to="/ems">
                                    <Button type="button" variant="outline">
                                        Open EMS
                                    </Button>
                                </Link>
                            </div>
                        </div>
                    </div>
                </HudCard>

                <HudCard
                    title="Current Preference"
                    subtitle="Saved in the shared workspace context"
                    action={
                        isLoading ? (
                            <span className="inline-flex items-center gap-2 text-xs text-hud-text-muted">
                                <RefreshCw size={14} className="animate-spin" />
                                Loading
                            </span>
                        ) : null
                    }
                >
                    <div className="space-y-4">
                        <div className="rounded-2xl border border-hud-border-primary bg-hud-accent-primary/10 p-4">
                            <p className="text-xs uppercase tracking-[0.24em] text-hud-accent-primary">
                                {session ? 'Signed-in Member' : 'Preferred Platform'}
                            </p>
                            {session ? (
                                <>
                                    <p className="mt-2 text-2xl font-semibold text-hud-text-primary">
                                        {session.displayName}
                                    </p>
                                    <p className="mt-2 text-sm text-hud-text-secondary">{session.email}</p>
                                </>
                            ) : (
                                <p className="mt-2 text-2xl font-semibold capitalize text-hud-text-primary">
                                    {workspace.preferredPlatformId.replace('-', ' ')}
                                </p>
                            )}
                        </div>

                        <div className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                            <p className="text-xs uppercase tracking-[0.22em] text-hud-text-muted">
                                Onboarding State
                            </p>
                            <p className="mt-2 text-sm font-medium text-hud-text-primary">
                                {connectionBootstrap?.summary.onboarding_stage ?? 'signup-required'}
                            </p>
                            <p className="mt-2 text-sm leading-6 text-hud-text-secondary">
                                {connectionBootstrap?.summary.next_step_message ??
                                    'Create an account first so platform connection state can be attached to a user.'}
                            </p>
                        </div>

                        {error && (
                            <div className="rounded-2xl border border-hud-accent-danger/40 bg-hud-accent-danger/10 p-4 text-sm leading-6 text-hud-text-secondary">
                                {error}
                            </div>
                        )}
                    </div>
                </HudCard>
            </section>

            <section className="grid gap-6 xl:grid-cols-[1fr_1.1fr]">
                <HudCard title="Onboarding Flow" subtitle="How platform selection feeds the product loop">
                    {session ? (
                        <div className="mb-4 rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4 text-sm leading-6 text-hud-text-secondary">
                            Current account: <span className="font-medium text-hud-text-primary">{session.displayName}</span>
                            {' '}with preferred source{' '}
                            <span className="font-medium capitalize text-hud-text-primary">
                                {session.preferredPlatformId.replace('-', ' ')}
                            </span>
                            .
                        </div>
                    ) : (
                        <div className="mb-4 rounded-2xl border border-dashed border-hud-border-secondary bg-hud-bg-primary/60 p-4 text-sm leading-6 text-hud-text-secondary">
                            Create an account first. Platform connection bootstrap needs a user to attach onboarding state.
                        </div>
                    )}

                    {catalog ? (
                        <div className="space-y-4">
                            {catalog.onboarding_flow.map((step, index) => (
                                <div
                                    key={step}
                                    className="flex items-start gap-4 rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4"
                                >
                                    <span className="flex h-9 w-9 items-center justify-center rounded-full bg-hud-accent-primary/10 text-sm font-semibold text-hud-accent-primary">
                                        {index + 1}
                                    </span>
                                    <p className="text-sm leading-6 text-hud-text-secondary">{step}</p>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <div className="text-sm leading-6 text-hud-text-secondary">
                            Platform onboarding guidance will appear here after the catalog loads.
                        </div>
                    )}
                </HudCard>

                <HudCard title="Supported Platforms" subtitle="Current product interpretation of platform roles">
                    {catalog ? (
                        <div className="grid gap-4">
                            {catalog.platforms.map((platform) => {
                                const isPreferred = workspace.preferredPlatformId === platform.platform_id
                                const currentConnection = connectionBootstrap?.connections.find(
                                    (connection) => connection.platform_id === platform.platform_id,
                                )

                                return (
                                    <div
                                        key={platform.platform_id}
                                        className={`rounded-2xl border p-5 transition-hud ${
                                            isPreferred
                                                ? 'border-hud-border-primary bg-hud-accent-primary/10'
                                                : 'border-hud-border-secondary bg-hud-bg-primary/70'
                                        }`}
                                    >
                                        <div className="flex flex-wrap items-start justify-between gap-3">
                                            <div>
                                                <div className="flex items-center gap-3">
                                                    <span className="rounded-2xl bg-hud-accent-primary/10 p-3 text-hud-accent-primary">
                                                        {platform.platform_id === 'spotify' ? (
                                                            <Disc3 size={20} />
                                                        ) : platform.platform_id === 'apple-music' ? (
                                                            <Sparkles size={20} />
                                                        ) : (
                                                            <Radio size={20} />
                                                        )}
                                                    </span>
                                                    <div>
                                                        <p className="text-lg font-semibold text-hud-text-primary">
                                                            {platform.display_name}
                                                        </p>
                                                        <p className="mt-1 text-xs uppercase tracking-[0.2em] text-hud-text-muted">
                                                            {stageLabel[platform.integration_stage] ?? platform.integration_stage}
                                                        </p>
                                                    </div>
                                                </div>
                                            </div>

                                            <Button
                                                type="button"
                                                variant={isPreferred ? 'primary' : 'outline'}
                                                onClick={() =>
                                                    updateWorkspace({ preferredPlatformId: platform.platform_id })
                                                }
                                            >
                                                {isPreferred ? (
                                                    <>
                                                        <CheckCircle2 size={16} />
                                                        Preferred
                                                    </>
                                                ) : (
                                                    'Use as Source'
                                                )}
                                            </Button>
                                            <Button
                                                type="button"
                                                variant={currentConnection?.connected ? 'secondary' : 'ghost'}
                                                disabled={!session || isMutating === platform.platform_id}
                                                onClick={() =>
                                                    handleConnectToggle(
                                                        platform.platform_id,
                                                        currentConnection?.connected ?? false,
                                                    )
                                                }
                                            >
                                                {isMutating === platform.platform_id
                                                    ? 'Working...'
                                                    : currentConnection?.connected
                                                      ? 'Disconnect'
                                                      : 'Start OAuth Sandbox'}
                                            </Button>
                                        </div>

                                        <div className="mt-5 grid gap-3 md:grid-cols-2">
                                            <div className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                                                <p className="text-xs uppercase tracking-[0.2em] text-hud-text-muted">
                                                    PMS Role
                                                </p>
                                                <p className="mt-2 text-sm leading-6 text-hud-text-primary">
                                                    {platform.pms_role}
                                                </p>
                                            </div>
                                            <div className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                                                <p className="text-xs uppercase tracking-[0.2em] text-hud-text-muted">
                                                    EMS Role
                                                </p>
                                                <p className="mt-2 text-sm leading-6 text-hud-text-primary">
                                                    {platform.ems_role}
                                                </p>
                                            </div>
                                        </div>

                                        <div className="mt-4 flex flex-wrap gap-2">
                                            <span className="rounded-full border border-hud-border-secondary px-3 py-1 text-xs text-hud-text-secondary">
                                                PMS import {platform.pms_import_supported ? 'enabled later' : 'not planned'}
                                            </span>
                                            <span className="rounded-full border border-hud-border-secondary px-3 py-1 text-xs text-hud-text-secondary">
                                                EMS collect {platform.ems_collection_supported ? 'enabled later' : 'not planned'}
                                            </span>
                                            <span className="rounded-full border border-hud-border-secondary px-3 py-1 text-xs text-hud-text-secondary">
                                                {platform.audio_feature_strategy}
                                            </span>
                                            {currentConnection?.connected && (
                                                <span className="rounded-full border border-hud-border-primary bg-hud-accent-primary/10 px-3 py-1 text-xs text-hud-accent-primary">
                                                    connected
                                                </span>
                                            )}
                                        </div>

                                        {currentConnection?.external_account_label && (
                                            <div className="mt-4 rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                                                <p className="text-xs uppercase tracking-[0.2em] text-hud-text-muted">
                                                    Connection Label
                                                </p>
                                                <p className="mt-2 text-sm text-hud-text-primary">
                                                    {currentConnection.external_account_label}
                                                </p>
                                            </div>
                                        )}

                                        <div className="mt-4 space-y-2 text-sm leading-6 text-hud-text-secondary">
                                            {platform.notes.map((note) => (
                                                <p key={note}>{note}</p>
                                            ))}
                                        </div>
                                    </div>
                                )
                            })}
                        </div>
                    ) : (
                        <div className="text-sm leading-6 text-hud-text-secondary">
                            Supported platform details will appear here after the API responds.
                        </div>
                    )}
                </HudCard>
            </section>
        </div>
    )
}

export default PlatformsPage
