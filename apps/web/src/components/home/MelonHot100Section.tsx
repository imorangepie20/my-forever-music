import { ArrowRight } from 'lucide-react'
import { Link } from 'react-router-dom'
import MelonChartRow from '@/components/home/MelonChartRow'
import { useMelonHot100 } from '@/hooks/useMelonHot100'

const SECTION_LIMIT = 10

const MelonHot100Section = () => {
    const state = useMelonHot100(SECTION_LIMIT)

    if (state.status === 'loading') {
        return (
            <section className="space-y-3">
                <header className="flex items-baseline justify-between">
                    <h2 className="text-lg font-semibold text-hud-text-primary">Melon Hot 100</h2>
                    <span className="text-xs text-hud-text-muted">Loading…</span>
                </header>
                <div className="grid gap-2 sm:grid-cols-2">
                    {Array.from({ length: SECTION_LIMIT }).map((_, index) => (
                        <div
                            key={index}
                            className="h-14 animate-pulse rounded-xl border border-hud-border-secondary bg-hud-bg-primary/60"
                        />
                    ))}
                </div>
            </section>
        )
    }

    if (state.status === 'empty' || state.status === 'error') {
        return null
    }

    return (
        <section className="space-y-4">
            <header className="flex items-baseline justify-between gap-3">
                <div>
                    <h2 className="text-lg font-semibold text-hud-text-primary">Melon Hot 100</h2>
                    {state.snapshotAt && (
                        <p className="mt-0.5 text-[11px] text-hud-text-muted">
                            Snapshot {new Date(state.snapshotAt).toLocaleString()}
                        </p>
                    )}
                </div>
                <Link
                    to="/melon-hot-100"
                    className="inline-flex items-center gap-1 text-xs font-semibold text-hud-accent-primary transition-hud hover:text-hud-accent-primary/80"
                >
                    View all 100
                    <ArrowRight size={14} />
                </Link>
            </header>
            <ol className="grid gap-2 sm:grid-cols-2">
                {state.tracks.map((track) => (
                    <li key={`${track.rank}-${track.melon_song_id ?? track.title}`}>
                        <MelonChartRow track={track} compact />
                    </li>
                ))}
            </ol>
        </section>
    )
}

export default MelonHot100Section
