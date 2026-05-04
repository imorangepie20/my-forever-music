interface MusicArtworkProps {
    imageUrl?: string | null
    seed: string
    label: string
    className?: string
}

const hashSeed = (seed: string) =>
    Array.from(seed).reduce((total, char) => total + char.charCodeAt(0), 0)

const MusicArtwork = ({ imageUrl, seed, label, className = '' }: MusicArtworkProps) => {
    const hue = hashSeed(seed) % 360
    const initials = label
        .split(' ')
        .filter(Boolean)
        .slice(0, 2)
        .map((value) => value[0]?.toUpperCase() ?? '')
        .join('')

    if (imageUrl) {
        return (
            <img
                src={imageUrl}
                alt={label}
                className={`h-full w-full object-cover ${className}`}
                loading="lazy"
            />
        )
    }

    return (
        <div
            className={`flex h-full w-full items-end justify-between overflow-hidden ${className}`}
            style={{
                background: `linear-gradient(145deg, hsla(${hue}, 72%, 54%, 0.95), hsla(${(hue + 68) % 360}, 70%, 16%, 1))`,
            }}
        >
            <span className="p-3 text-2xl font-semibold tracking-[0.24em] text-white/85">{initials || 'MF'}</span>
            <span className="p-3 text-[10px] uppercase tracking-[0.26em] text-white/70">My Forever Music</span>
        </div>
    )
}

export default MusicArtwork
