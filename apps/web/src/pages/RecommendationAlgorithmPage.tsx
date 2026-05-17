import { ArrowLeft, Library, Newspaper, ShieldCheck, Sparkles, Workflow } from 'lucide-react'
import { Link } from 'react-router-dom'
import HudCard from '@/components/common/HudCard'

const RecommendationAlgorithmPage = () => {
    return (
        <div className="space-y-6">
            <header className="flex items-center justify-between gap-3">
                <Link
                    to="/"
                    className="inline-flex items-center gap-2 text-sm text-hud-text-secondary transition-hud hover:text-hud-text-primary"
                >
                    <ArrowLeft size={16} />
                    Back to home
                </Link>
                <span className="text-xs uppercase tracking-[0.28em] text-hud-text-muted">
                    Recommendation pipeline
                </span>
            </header>

            <section className="relative overflow-hidden rounded-3xl border border-hud-border-secondary bg-gradient-to-br from-hud-bg-primary/95 via-hud-bg-primary/80 to-hud-bg-primary/95 px-8 py-10">
                <p className="text-[11px] uppercase tracking-[0.32em] text-hud-accent-primary">
                    Three signals, one feed
                </p>
                <h1 className="mt-3 max-w-3xl text-3xl font-semibold tracking-tight text-hud-text-primary sm:text-4xl">
                    Your library, an editorial reading list, and a transparent six-axis ranker.
                </h1>
                <p className="mt-4 max-w-3xl text-sm leading-7 text-hud-text-secondary sm:text-base">
                    My Forever Music splits recommendation into three stages so the inputs stay legible:
                    a personal library (PMS), an editorially-curated pool (EMS), and a personalized
                    ranker (GMS). Every playlist you see on the home page is scored against six explicit
                    axes — no opaque black box.
                </p>
            </section>

            <section className="grid gap-4 md:grid-cols-3">
                <HudCard
                    title="PMS — Personal Music Service"
                    subtitle="Your taste reference"
                >
                    <div className="flex items-start gap-3">
                        <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-hud-accent-primary/10 text-hud-accent-primary">
                            <Library size={20} />
                        </span>
                        <p className="text-sm leading-6 text-hud-text-secondary">
                            We import the playlists you already love from Spotify and TIDAL. Audio
                            features (energy, danceability, mood) are resolved per track so the rest of
                            the pipeline has a stable signal to compare against.
                        </p>
                    </div>
                </HudCard>

                <HudCard
                    title="EMS — Editorial Music Service"
                    subtitle="Discovery from real journalism"
                >
                    <div className="flex items-start gap-3">
                        <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-hud-accent-info/10 text-hud-accent-info">
                            <Newspaper size={20} />
                        </span>
                        <p className="text-sm leading-6 text-hud-text-secondary">
                            A scheduled pipeline reads RSS feeds from Pitchfork, Stereogum,
                            BrooklynVegan, FACT, The FADER, NME and similar publications, extracts
                            track mentions, and resolves them against Spotify / TIDAL to fill the
                            EMS pool with editor-vetted candidates.
                        </p>
                    </div>
                </HudCard>

                <HudCard
                    title="GMS — Group Music Service"
                    subtitle="Personalized ranker"
                >
                    <div className="flex items-start gap-3">
                        <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-400/10 text-amber-300">
                            <Sparkles size={20} />
                        </span>
                        <p className="text-sm leading-6 text-hud-text-secondary">
                            For each user, GMS pulls EMS candidates and ranks them against the PMS
                            profile. The output is the "Recommended for you" surface — composite
                            scores you can trace back to specific axes, not a one-number popularity
                            proxy.
                        </p>
                    </div>
                </HudCard>
            </section>

            <HudCard
                title="The six axes"
                subtitle="Every recommendation comes with explicit evidence"
            >
                <ul className="grid gap-3 sm:grid-cols-2">
                    {SIX_AXES.map((axis) => (
                        <li key={axis.name} className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                            <p className="text-sm font-semibold text-hud-text-primary">{axis.name}</p>
                            <p className="mt-1 text-xs leading-5 text-hud-text-secondary">{axis.description}</p>
                        </li>
                    ))}
                </ul>
            </HudCard>

            <section className="grid gap-4 md:grid-cols-2">
                <HudCard
                    title="No mock data in production paths"
                    subtitle="Real provider, real PCM, real failure messages"
                >
                    <div className="flex items-start gap-3">
                        <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-400/10 text-emerald-300">
                            <ShieldCheck size={20} />
                        </span>
                        <p className="text-sm leading-6 text-hud-text-secondary">
                            We refuse to silently substitute synthetic data when a real provider call
                            fails. Errors surface with platform-specific guidance instead of fake
                            success — you can always tell what the system actually did.
                        </p>
                    </div>
                </HudCard>

                <HudCard
                    title="Feedback closes the loop"
                    subtitle="Likes and saves rewrite your profile"
                >
                    <div className="flex items-start gap-3">
                        <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-fuchsia-400/10 text-fuchsia-300">
                            <Workflow size={20} />
                        </span>
                        <p className="text-sm leading-6 text-hud-text-secondary">
                            Likes from the dock, recommendation feedback, and playback completions all
                            feed back into the PMS profile and the GMS ranker. Recommendations are
                            never frozen — they move with you.
                        </p>
                    </div>
                </HudCard>
            </section>

            <div className="flex flex-wrap items-center gap-3">
                <Link
                    to="/ems"
                    className="inline-flex items-center gap-2 rounded-full border border-hud-border-secondary bg-hud-bg-primary/80 px-4 py-2 text-sm text-hud-text-secondary transition-hud hover:border-hud-border-primary hover:text-hud-text-primary"
                >
                    Explore the EMS workspace
                </Link>
                <Link
                    to="/gms-preview"
                    className="inline-flex items-center gap-2 rounded-full border border-hud-border-secondary bg-hud-bg-primary/80 px-4 py-2 text-sm text-hud-text-secondary transition-hud hover:border-hud-border-primary hover:text-hud-text-primary"
                >
                    Run a GMS preview
                </Link>
                <Link
                    to="/platforms"
                    className="inline-flex items-center gap-2 rounded-full border border-hud-border-secondary bg-hud-bg-primary/80 px-4 py-2 text-sm text-hud-text-secondary transition-hud hover:border-hud-border-primary hover:text-hud-text-primary"
                >
                    Connect your streaming platform
                </Link>
            </div>
        </div>
    )
}

const SIX_AXES = [
    {
        name: 'Affinity',
        description: 'How closely a candidate matches the energy/mood/genre fingerprint of your PMS library.',
    },
    {
        name: 'Novelty',
        description: 'Bias toward tracks and playlists you have not encountered yet — the opposite of an echo chamber.',
    },
    {
        name: 'Coherence',
        description: 'Internal consistency of the playlist itself — does it hold a mood, or is it a random pile?',
    },
    {
        name: 'Diversity',
        description: 'How much the candidate broadens your current pool vs. duplicating what you already have.',
    },
    {
        name: 'Redundancy',
        description: 'Penalty when a candidate overlaps too much with what we already recommended this session.',
    },
    {
        name: 'Confidence',
        description: 'Signal strength — fewer features filled, lower confidence, even if the affinity score is high.',
    },
]

export default RecommendationAlgorithmPage
