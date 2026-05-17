import { useEffect, useState } from 'react'
import { Activity, ArrowRight, Globe, Server, Sparkles } from 'lucide-react'
import { Link } from 'react-router-dom'
import Button from '@/components/common/Button'
import GmsRecommendedPlaylistsSection from '@/components/home/GmsRecommendedPlaylistsSection'
import HeroEqBanner from '@/components/home/HeroEqBanner'
import LatestTracksSection from '@/components/home/LatestTracksSection'
import PopularPlaylistsSection from '@/components/home/PopularPlaylistsSection'
import HudCard from '@/components/common/HudCard'
import StatCard from '@/components/common/StatCard'
import { useAuthSession } from '@/contexts/AuthSessionContext'
import { useRecommendationWorkspace } from '@/contexts/RecommendationWorkspaceContext'
import { ApiError, fetchSystemInfo, getAiDocsUrl, getApiDocsUrl } from '@/services/api'
import type { SystemInfoResponse } from '@/types/api'

const architectureCards = [
    {
        title: 'Web Shell',
        subtitle: 'React + Vite',
        body: 'The browser app now exposes a focused control room and a GMS preview workflow instead of template demo routes.',
        icon: <Activity size={22} />,
    },
    {
        title: 'API Bridge',
        subtitle: 'Spring Boot',
        body: 'The main API exposes /api/v1/gms/recommendations/preview and forwards snake_case payloads to the AI service.',
        icon: <Globe size={22} />,
    },
    {
        title: 'AI Preview',
        subtitle: 'FastAPI',
        body: 'Preview responses are rule-based for now, which keeps the contract stable while real ranking logic is still evolving.',
        icon: <Sparkles size={22} />,
    },
]

const deliveryTracks = [
    'Signup and primary platform onboarding',
    'Platform connection and playlist intake',
    'PMS approval events feeding EMS and GMS model loops',
]

const HomePage = () => {
    const { session } = useAuthSession()
    const { workspace } = useRecommendationWorkspace()
    const [systemInfo, setSystemInfo] = useState<SystemInfoResponse | null>(null)
    const [statusError, setStatusError] = useState<string | null>(null)

    useEffect(() => {
        const controller = new AbortController()

        fetchSystemInfo(controller.signal)
            .then((response) => {
                setSystemInfo(response)
                setStatusError(null)
            })
            .catch((error: unknown) => {
                if (error instanceof DOMException && error.name === 'AbortError') {
                    return
                }

                if (error instanceof ApiError) {
                    setStatusError(error.message)
                    return
                }

                setStatusError('Unable to reach the Spring Boot system endpoint.')
            })

        return () => controller.abort()
    }, [])

    return (
        <div className="space-y-6">
            <HeroEqBanner />

            <LatestTracksSection />

            <PopularPlaylistsSection />

            <GmsRecommendedPlaylistsSection />

            <section className="grid gap-6 xl:grid-cols-[1.3fr_0.9fr]">
                <HudCard className="overflow-hidden">
                    <div className="relative">
                        <div className="absolute inset-x-0 top-0 h-40 rounded-3xl bg-gradient-to-r from-hud-accent-primary/20 via-cyan-300/10 to-hud-accent-secondary/15 blur-3xl" />
                        <div className="relative">
                            <p className="text-xs font-semibold uppercase tracking-[0.28em] text-hud-accent-primary">
                                Delivery Snapshot
                            </p>
                            <h2 className="mt-4 max-w-3xl text-3xl font-semibold tracking-tight text-hud-text-primary sm:text-4xl">
                                Web, Spring Boot, and FastAPI are finally speaking the same recommendation language.
                            </h2>
                            <p className="mt-4 max-w-2xl text-base leading-7 text-hud-text-secondary">
                                The current frontend is intentionally narrow: it focuses on the GMS preview loop so we can
                                validate contract shape, service boundaries, and delivery flow before rebuilding the full
                                PMS and EMS journeys.
                            </p>

                            <div className="mt-8 flex flex-wrap gap-3">
                                {session ? (
                                    <Link
                                        to={session.nextStepPath || '/platforms'}
                                        className="btn-glow inline-flex items-center gap-2 rounded-xl bg-hud-accent-primary px-5 py-3 text-sm font-semibold text-hud-bg-primary transition-hud"
                                    >
                                        Continue Onboarding
                                        <ArrowRight size={16} />
                                    </Link>
                                ) : (
                                    <Link
                                        to="/signup"
                                        className="btn-glow inline-flex items-center gap-2 rounded-xl bg-hud-accent-primary px-5 py-3 text-sm font-semibold text-hud-bg-primary transition-hud"
                                    >
                                        Start Signup
                                        <ArrowRight size={16} />
                                    </Link>
                                )}
                                {!session && (
                                    <Link
                                        to="/login"
                                        className="inline-flex items-center gap-2 rounded-xl border border-hud-border-secondary bg-hud-bg-primary/80 px-5 py-3 text-sm font-medium text-hud-text-secondary transition-hud hover:border-hud-border-primary hover:text-hud-text-primary"
                                    >
                                        Sign In
                                    </Link>
                                )}
                                <Link
                                    to="/platforms"
                                    className="inline-flex items-center gap-2 rounded-xl border border-hud-border-secondary bg-hud-bg-primary/80 px-5 py-3 text-sm font-medium text-hud-text-secondary transition-hud hover:border-hud-border-primary hover:text-hud-text-primary"
                                >
                                    Open Platform Intake
                                </Link>
                                <Link
                                    to="/gms-preview"
                                    className="inline-flex items-center gap-2 rounded-xl border border-hud-border-secondary bg-hud-bg-primary/80 px-5 py-3 text-sm font-medium text-hud-text-secondary transition-hud hover:border-hud-border-primary hover:text-hud-text-primary"
                                >
                                    Open GMS Preview
                                </Link>
                                <a
                                    href={getApiDocsUrl()}
                                    target="_blank"
                                    rel="noreferrer"
                                    className="inline-flex items-center gap-2 rounded-xl border border-hud-border-secondary bg-hud-bg-primary/80 px-5 py-3 text-sm font-medium text-hud-text-secondary transition-hud hover:border-hud-border-primary hover:text-hud-text-primary"
                                >
                                    Spring Boot Docs
                                </a>
                                <a
                                    href={getAiDocsUrl()}
                                    target="_blank"
                                    rel="noreferrer"
                                    className="inline-flex items-center gap-2 rounded-xl border border-hud-border-secondary bg-hud-bg-primary/80 px-5 py-3 text-sm font-medium text-hud-text-secondary transition-hud hover:border-hud-border-primary hover:text-hud-text-primary"
                                >
                                    FastAPI Docs
                                </a>
                            </div>
                        </div>
                    </div>
                </HudCard>

                <HudCard
                    title="System Signal"
                    subtitle="Live check against /api/v1/system/info"
                >
                    {systemInfo ? (
                        <div className="space-y-4">
                            <div className="rounded-2xl border border-hud-border-primary bg-hud-accent-primary/10 p-4">
                                <p className="text-xs uppercase tracking-[0.24em] text-hud-accent-primary">
                                    {systemInfo.service}
                                </p>
                                <p className="mt-2 text-2xl font-semibold text-hud-text-primary">
                                    {systemInfo.status}
                                </p>
                                <p className="mt-2 text-sm leading-6 text-hud-text-secondary">
                                    {systemInfo.message}
                                </p>
                            </div>
                            <div className="flex items-center gap-3 rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                                <span className="rounded-xl bg-hud-accent-info/10 p-2 text-hud-accent-info">
                                    <Server size={18} />
                                </span>
                                <div>
                                    <p className="text-xs uppercase tracking-[0.22em] text-hud-text-muted">
                                        Timestamp
                                    </p>
                                    <p className="mt-1 text-sm text-hud-text-primary">
                                        {new Date(systemInfo.timestamp).toLocaleString()}
                                    </p>
                                </div>
                            </div>
                        </div>
                    ) : (
                        <div className="space-y-3">
                            <div className="rounded-2xl border border-dashed border-hud-border-secondary bg-hud-bg-primary/60 p-5 text-sm leading-6 text-hud-text-secondary">
                                {statusError ??
                                    'Waiting for the Spring Boot API. Start services/api and refresh this page to see live bootstrap status.'}
                            </div>
                        </div>
                    )}
                </HudCard>
            </section>

            <section className="grid gap-6 md:grid-cols-2 xl:grid-cols-4">
                <StatCard
                    title="PMS Context"
                    value={workspace.playlistId ? 'Ready' : 'Open'}
                    icon={<Activity size={22} />}
                    variant="primary"
                />
                <StatCard
                    title="EMS Model"
                    value={`${workspace.energyLevel}/${workspace.familiarityBias}`}
                    icon={<Globe size={22} />}
                    variant="secondary"
                />
                <StatCard
                    title="GMS Limit"
                    value={workspace.limit}
                    icon={<Sparkles size={22} />}
                    variant="warning"
                />
                <StatCard
                    title="Current Mood"
                    value={workspace.mood}
                    icon={<Server size={22} />}
                    variant="default"
                />
            </section>

            <section className="grid gap-6 xl:grid-cols-[1.2fr_1fr]">
                <HudCard title="Architecture Focus" subtitle="What is already wired">
                    <div className="grid gap-4 md:grid-cols-3">
                        {architectureCards.map((card) => (
                            <div
                                key={card.title}
                                className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-5"
                            >
                                <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-hud-accent-primary/10 text-hud-accent-primary">
                                    {card.icon}
                                </div>
                                <p className="mt-4 text-lg font-semibold text-hud-text-primary">{card.title}</p>
                                <p className="mt-1 text-sm uppercase tracking-[0.18em] text-hud-text-muted">
                                    {card.subtitle}
                                </p>
                                <p className="mt-4 text-sm leading-6 text-hud-text-secondary">{card.body}</p>
                            </div>
                        ))}
                    </div>
                </HudCard>

                <HudCard title="Next Delivery Track" subtitle="Most natural follow-up work">
                    <div className="space-y-4">
                        {deliveryTracks.map((track, index) => (
                            <div
                                key={track}
                                className="flex items-start gap-4 rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4"
                            >
                                <span className="flex h-9 w-9 items-center justify-center rounded-full bg-hud-accent-primary/10 text-sm font-semibold text-hud-accent-primary">
                                    {index + 1}
                                </span>
                                <div>
                                    <p className="text-sm font-medium text-hud-text-primary">{track}</p>
                                    <p className="mt-1 text-sm leading-6 text-hud-text-secondary">
                                        Keep the rebuild narrow until the preview contract, API bridge, and web shell feel reliable.
                                    </p>
                                </div>
                            </div>
                        ))}
                    </div>
                    <div className="mt-5 flex flex-wrap gap-3">
                        <Link to="/pms">
                            <Button type="button" variant="outline">
                                Open PMS
                            </Button>
                        </Link>
                        <Link to="/platforms">
                            <Button type="button" variant="outline">
                                Open Platforms
                            </Button>
                        </Link>
                        <Link to="/ems">
                            <Button type="button" variant="outline">
                                Open EMS
                            </Button>
                        </Link>
                        <Link to="/gms-preview">
                            <Button type="button" variant="primary" glow>
                                Open GMS
                            </Button>
                        </Link>
                    </div>
                </HudCard>
            </section>
        </div>
    )
}

export default HomePage
