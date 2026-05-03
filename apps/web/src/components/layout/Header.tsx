import { Activity, ExternalLink, Globe, Menu, Sparkles } from 'lucide-react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import Button from '@/components/common/Button'
import { useAuthSession } from '@/contexts/AuthSessionContext'
import { useRecommendationWorkspace } from '@/contexts/RecommendationWorkspaceContext'
import { getAiDocsUrl, getApiConnectionLabel, getApiDocsUrl } from '@/services/api'

interface HeaderProps {
    onMenuToggle: () => void
}

const pageCopy: Record<string, { title: string; subtitle: string }> = {
    '/': {
        title: 'My Forever Music Control Room',
        subtitle: 'Rebuild status, service links, and the current delivery track.',
    },
    '/platforms': {
        title: 'Platform Intake Workspace',
        subtitle: 'Choose the streaming source that will feed PMS and define how audio features will be resolved.',
    },
    '/platforms/oauth/authorize': {
        title: 'Platform OAuth Approval',
        subtitle: 'Review the sandbox provider consent step before the callback completes.',
    },
    '/platforms/oauth/callback': {
        title: 'Platform OAuth Callback',
        subtitle: 'Finalize the sandbox authorization and move into the next onboarding stage.',
    },
    '/pms': {
        title: 'PMS Seed Workspace',
        subtitle: 'Capture playlist and catalog anchors before we shape the emotional signal.',
    },
    '/ems': {
        title: 'EMS Signal Workspace',
        subtitle: 'Tune mood, energy, and familiarity before the GMS preview call.',
    },
    '/gms-preview': {
        title: 'GMS Recommendation Preview',
        subtitle: 'Call the Spring Boot bridge and inspect the AI preview response end to end.',
    },
}

const Header = ({ onMenuToggle }: HeaderProps) => {
    const location = useLocation()
    const navigate = useNavigate()
    const { session, clearSession } = useAuthSession()
    const { resetWorkspace } = useRecommendationWorkspace()
    const currentPage = pageCopy[location.pathname] ?? {
        title: 'My Forever Music',
        subtitle: 'Music service rebuild workspace.',
    }

    const handleSignOut = () => {
        clearSession()
        resetWorkspace()
        navigate('/login')
    }

    return (
        <header className="sticky top-0 z-40 border-b border-hud-border-secondary bg-hud-bg-secondary/85 backdrop-blur-xl">
            <div className="flex flex-col gap-4 px-4 py-4 sm:px-6 lg:flex-row lg:items-center lg:justify-between lg:px-8">
                <div className="flex items-start gap-3">
                    <button
                        onClick={onMenuToggle}
                        className="mt-1 rounded-lg border border-hud-border-secondary bg-hud-bg-primary/80 p-2 text-hud-text-secondary transition-hud hover:border-hud-border-primary hover:text-hud-text-primary lg:hidden"
                        aria-label="Toggle navigation"
                    >
                        <Menu size={20} />
                    </button>

                    <div>
                        <div className="flex items-center gap-2">
                            <span className="rounded-full border border-hud-border-primary bg-hud-accent-primary/10 px-2.5 py-1 text-[11px] font-semibold uppercase tracking-[0.24em] text-hud-accent-primary">
                                Rebuild
                            </span>
                            <span className="rounded-full border border-hud-border-secondary bg-hud-bg-primary/80 px-2.5 py-1 text-[11px] font-medium text-hud-text-muted">
                                {getApiConnectionLabel()}
                            </span>
                        </div>
                        <h1 className="mt-3 text-2xl font-semibold tracking-tight text-hud-text-primary">
                            {currentPage.title}
                        </h1>
                        <p className="mt-1 max-w-2xl text-sm text-hud-text-secondary">
                            {currentPage.subtitle}
                        </p>
                    </div>
                </div>

                <div className="flex flex-wrap items-center gap-3">
                    {session ? (
                        <div className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/80 px-4 py-3">
                            <p className="text-xs uppercase tracking-[0.2em] text-hud-text-muted">
                                Signed In
                            </p>
                            <p className="mt-1 text-sm font-medium text-hud-text-primary">
                                {session.displayName}
                            </p>
                        </div>
                    ) : (
                        <Link to="/login">
                            <Button type="button" variant="ghost">
                                Sign In
                            </Button>
                        </Link>
                    )}

                    {!session && (
                        <Link to="/signup">
                            <Button type="button" variant="outline">
                                Sign Up
                            </Button>
                        </Link>
                    )}

                    <div className="hidden min-w-[220px] rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/80 px-4 py-3 md:block">
                        <div className="flex items-center gap-3">
                            <div className="rounded-xl bg-hud-accent-primary/10 p-2 text-hud-accent-primary">
                                <Globe size={18} />
                            </div>
                            <div>
                                <p className="text-xs uppercase tracking-[0.2em] text-hud-text-muted">
                                    Active Chain
                                </p>
                                <p className="mt-1 text-sm text-hud-text-primary">
                                    {'Web -> Spring Boot -> FastAPI'}
                                </p>
                            </div>
                        </div>
                    </div>

                    <a
                        href={getApiDocsUrl()}
                        target="_blank"
                        rel="noreferrer"
                        className="inline-flex items-center gap-2 rounded-xl border border-hud-border-secondary bg-hud-bg-primary/80 px-4 py-2.5 text-sm font-medium text-hud-text-secondary transition-hud hover:border-hud-border-primary hover:text-hud-text-primary"
                    >
                        <Activity size={16} />
                        API Docs
                        <ExternalLink size={14} />
                    </a>

                    <a
                        href={getAiDocsUrl()}
                        target="_blank"
                        rel="noreferrer"
                        className="inline-flex items-center gap-2 rounded-xl border border-hud-border-secondary bg-hud-bg-primary/80 px-4 py-2.5 text-sm font-medium text-hud-text-secondary transition-hud hover:border-hud-border-primary hover:text-hud-text-primary"
                    >
                        <Sparkles size={16} />
                        AI Docs
                        <ExternalLink size={14} />
                    </a>

                    {session && (
                        <Button type="button" variant="ghost" onClick={handleSignOut}>
                            Sign Out
                        </Button>
                    )}
                </div>
            </div>
        </header>
    )
}

export default Header
