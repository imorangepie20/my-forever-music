import { Music2 } from 'lucide-react'
import type { PlaybackMediaItem } from '@/lib/musicPlayback'
import { formatDuration } from '@/lib/musicPlayback'

interface QueueRailProps {
    queue: PlaybackMediaItem[]
    currentIndex: number
    accentHex: string
}

const QueueRail = ({ queue, currentIndex, accentHex }: QueueRailProps) => {
    if (queue.length === 0) {
        return (
            <aside className="hidden h-full w-72 shrink-0 flex-col border-r border-white/10 bg-black/40 px-5 py-6 backdrop-blur md:flex">
                <h2 className="text-xs uppercase tracking-[0.28em] text-white/50">Queue</h2>
                <p className="mt-4 text-sm text-white/70">No queued tracks.</p>
            </aside>
        )
    }

    return (
        <aside className="hidden h-full w-72 shrink-0 flex-col border-r border-white/10 bg-black/40 px-5 py-6 backdrop-blur md:flex">
            <h2 className="text-xs uppercase tracking-[0.28em] text-white/50">
                {queue.length === 1 ? 'Now playing' : 'Queue'}
            </h2>
            <p className="mt-1 text-sm text-white/70">
                {currentIndex + 1} / {queue.length}
            </p>
            <ol className="mt-5 flex-1 space-y-2 overflow-y-auto pr-1">
                {queue.map((item, index) => {
                    const isActive = index === currentIndex
                    const duration = formatDuration(item.durationMs)
                    return (
                        <li
                            key={`${item.id}-${index}`}
                            className={`flex items-start gap-3 rounded-lg border px-3 py-2 transition-colors ${
                                isActive
                                    ? 'border-white/30 bg-white/10 text-white'
                                    : 'border-transparent text-white/70 hover:bg-white/5'
                            }`}
                            style={isActive ? { borderColor: accentHex } : undefined}
                        >
                            <span
                                className="mt-1 flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-[10px] font-semibold"
                                style={isActive
                                    ? { background: accentHex, color: '#0b1220' }
                                    : { background: 'rgba(255,255,255,0.08)', color: 'rgba(255,255,255,0.7)' }}
                            >
                                {isActive ? <Music2 size={12} /> : index + 1}
                            </span>
                            <div className="min-w-0 flex-1">
                                <p className="truncate text-sm font-medium">{item.title}</p>
                                <p className="mt-0.5 truncate text-xs text-white/60">{item.subtitle}</p>
                                {duration && (
                                    <p className="mt-1 font-mono text-[10px] text-white/40">{duration}</p>
                                )}
                            </div>
                        </li>
                    )
                })}
            </ol>
        </aside>
    )
}

export default QueueRail
