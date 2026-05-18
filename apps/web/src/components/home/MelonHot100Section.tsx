import { ArrowRight, Loader2, Play } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Button from '@/components/common/Button'
import MelonChartRow from '@/components/home/MelonChartRow'
import { useAuthSession } from '@/contexts/AuthSessionContext'
import { usePlayback } from '@/contexts/PlaybackContext'
import { useMelonHot100 } from '@/hooks/useMelonHot100'
import { toMelonHot100PlaybackItem } from '@/lib/melonPlayback'
import { ApiError, fetchMelonHot100 } from '@/services/api'

const SECTION_LIMIT = 10

const MelonHot100Section = () => {
    const state = useMelonHot100(SECTION_LIMIT)
    const { session } = useAuthSession()
    const playback = usePlayback()
    const navigate = useNavigate()
    const [isPlayingAll, setIsPlayingAll] = useState(false)
    const [playError, setPlayError] = useState<string | null>(null)

    const handlePlayAll = async () => {
        if (!session?.userId) {
            navigate('/signin')
            return
        }
        setIsPlayingAll(true)
        setPlayError(null)
        try {
            const response = await fetchMelonHot100(null, true)
            if (response.tracks.length === 0) {
                setPlayError('Melon Hot 100 데이터가 아직 없습니다.')
                return
            }
            await playback.playQueue(response.tracks.map(toMelonHot100PlaybackItem), 0)
        } catch (error: unknown) {
            const message = error instanceof ApiError
                ? error.message
                : error instanceof Error
                    ? error.message
                    : 'Melon Hot 100 연속 재생을 시작할 수 없습니다.'
            setPlayError(message)
        } finally {
            setIsPlayingAll(false)
        }
    }

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
                            스냅샷 {new Date(state.snapshotAt).toLocaleString()}
                        </p>
                    )}
                </div>
                <div className="flex flex-wrap items-center justify-end gap-2">
                    <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={() => void handlePlayAll()}
                        disabled={isPlayingAll}
                    >
                        {isPlayingAll ? <Loader2 size={15} className="animate-spin" /> : <Play size={15} />}
                        100곡 연속 재생
                    </Button>
                    <Link
                        to="/melon-hot-100"
                        className="inline-flex items-center gap-1 text-xs font-semibold text-hud-accent-primary transition-hud hover:text-hud-accent-primary/80"
                    >
                        전체 보기
                        <ArrowRight size={14} />
                    </Link>
                </div>
            </header>
            {playError && (
                <div className="rounded-2xl border border-hud-accent-warning/40 bg-hud-accent-warning/10 p-3 text-xs leading-5 text-hud-text-secondary">
                    {playError}
                </div>
            )}
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
