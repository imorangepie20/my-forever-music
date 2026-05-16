import MusicArtwork from '@/components/music/MusicArtwork'

interface FullscreenCoverProps {
    imageUrl?: string | null
    seed: string
    label: string
    accentHex: string
}

const FullscreenCover = ({ imageUrl, seed, label, accentHex }: FullscreenCoverProps) => {
    return (
        <div className="absolute inset-0 overflow-hidden">
            {imageUrl ? (
                <>
                    <img
                        src={imageUrl}
                        alt=""
                        aria-hidden
                        className="absolute inset-0 h-full w-full scale-110 object-cover blur-2xl opacity-60"
                    />
                    <img
                        src={imageUrl}
                        alt={label}
                        className="absolute inset-0 m-auto h-[78vh] w-[78vh] max-w-[92vw] rounded-3xl object-cover shadow-[0_30px_90px_-20px_rgba(0,0,0,0.7)]"
                    />
                </>
            ) : (
                <div className="absolute inset-0">
                    <MusicArtwork imageUrl={null} seed={seed} label={label} />
                </div>
            )}
            <div
                className="absolute inset-0 pointer-events-none"
                style={{
                    background: `radial-gradient(circle at 50% 60%, ${accentHex}26 0%, rgba(0,0,0,0.35) 45%, rgba(0,0,0,0.85) 100%)`,
                }}
            />
        </div>
    )
}

export default FullscreenCover
