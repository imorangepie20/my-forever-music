import { Home, RotateCcw } from 'lucide-react'
import { Link } from 'react-router-dom'

const NotFoundPage = () => {
    return (
        <div className="flex min-h-screen items-center justify-center px-6 py-12">
            <div className="max-w-xl text-center">
                <p className="text-xs font-semibold uppercase tracking-[0.3em] text-hud-accent-primary">404</p>
                <h1 className="mt-4 text-4xl font-semibold tracking-tight text-hud-text-primary">
                    This route is outside the rebuild shell.
                </h1>
                <p className="mt-4 text-base leading-7 text-hud-text-secondary">
                    The frontend has been narrowed down to the current music rebuild flow, so the page you requested
                    is not part of the active product shell yet.
                </p>

                <div className="mt-8 flex flex-wrap justify-center gap-3">
                    <Link
                        to="/"
                        className="btn-glow inline-flex items-center gap-2 rounded-xl bg-hud-accent-primary px-5 py-3 text-sm font-semibold text-hud-bg-primary transition-hud"
                    >
                        <Home size={16} />
                        Back to Overview
                    </Link>
                    <button
                        onClick={() => window.history.back()}
                        className="inline-flex items-center gap-2 rounded-xl border border-hud-border-secondary bg-hud-bg-primary/80 px-5 py-3 text-sm font-medium text-hud-text-secondary transition-hud hover:border-hud-border-primary hover:text-hud-text-primary"
                    >
                        <RotateCcw size={16} />
                        Go Back
                    </button>
                </div>
            </div>
        </div>
    )
}

export default NotFoundPage
