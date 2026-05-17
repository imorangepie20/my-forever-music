import { ArrowRight, Library, Newspaper, Sparkles } from 'lucide-react'
import { Link } from 'react-router-dom'

const AlgorithmIntroSection = () => {
    return (
        <section className="relative overflow-hidden rounded-3xl border border-hud-border-secondary bg-gradient-to-br from-hud-bg-primary/95 via-hud-bg-primary/80 to-hud-bg-primary/95 px-6 py-7 sm:px-8">
            <div className="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
                <div className="space-y-3">
                    <p className="text-[11px] uppercase tracking-[0.32em] text-hud-accent-primary">
                        How recommendations work
                    </p>
                    <h2 className="text-2xl font-semibold text-hud-text-primary sm:text-3xl">
                        Three signals — your library, our editors, the math in between.
                    </h2>
                    <p className="max-w-2xl text-sm leading-6 text-hud-text-secondary">
                        We blend your imported playlists (PMS), an editorial pool curated from music
                        journalism (EMS), and a six-axis scoring pass (GMS) to surface playlists you'll
                        actually want to play.
                    </p>
                </div>
                <Link
                    to="/about/recommendation"
                    className="inline-flex shrink-0 items-center gap-2 self-start rounded-full bg-hud-accent-primary px-5 py-2.5 text-sm font-semibold text-hud-bg-primary transition-hud hover:bg-hud-accent-primary/90 lg:self-auto"
                >
                    Read the breakdown
                    <ArrowRight size={16} />
                </Link>
            </div>
            <div className="mt-6 grid gap-3 sm:grid-cols-3">
                <div className="flex items-start gap-3 rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                    <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-hud-accent-primary/10 text-hud-accent-primary">
                        <Library size={18} />
                    </span>
                    <div>
                        <p className="text-sm font-semibold text-hud-text-primary">PMS</p>
                        <p className="mt-1 text-xs leading-5 text-hud-text-secondary">
                            Your imported library becomes the personal taste reference.
                        </p>
                    </div>
                </div>
                <div className="flex items-start gap-3 rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                    <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-hud-accent-info/10 text-hud-accent-info">
                        <Newspaper size={18} />
                    </span>
                    <div>
                        <p className="text-sm font-semibold text-hud-text-primary">EMS</p>
                        <p className="mt-1 text-xs leading-5 text-hud-text-secondary">
                            Editorial pool grown from RSS feeds (Pitchfork, Stereogum, NME…).
                        </p>
                    </div>
                </div>
                <div className="flex items-start gap-3 rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                    <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-amber-400/10 text-amber-300">
                        <Sparkles size={18} />
                    </span>
                    <div>
                        <p className="text-sm font-semibold text-hud-text-primary">GMS</p>
                        <p className="mt-1 text-xs leading-5 text-hud-text-secondary">
                            Six-axis ranking produces the playlists you see on the home page.
                        </p>
                    </div>
                </div>
            </div>
        </section>
    )
}

export default AlgorithmIntroSection
