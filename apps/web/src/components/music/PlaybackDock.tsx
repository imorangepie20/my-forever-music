import { ExternalLink, X } from 'lucide-react'
import Button from '@/components/common/Button'
import MusicArtwork from '@/components/music/MusicArtwork'
import { usePlayback } from '@/contexts/PlaybackContext'
import { resolveSpotifyEmbedUrl } from '@/lib/musicPlayback'

const PlaybackDock = () => {
    const { currentItem, clearItem } = usePlayback()

    if (!currentItem) {
        return null
    }

    const spotifyEmbedUrl = resolveSpotifyEmbedUrl(currentItem)
    const hasAudioPreview = !spotifyEmbedUrl && Boolean(currentItem.previewUrl)

    return (
        <div className="fixed bottom-0 left-0 right-0 z-40 border-t border-hud-border-secondary bg-hud-bg-secondary/95 backdrop-blur-xl">
            <div className="mx-auto grid max-w-[1600px] gap-4 px-4 py-4 lg:grid-cols-[320px_minmax(0,1fr)] lg:px-8">
                <div className="flex items-center gap-4 rounded-[24px] border border-hud-border-secondary bg-hud-bg-primary/85 p-4">
                    <div className="h-20 w-20 overflow-hidden rounded-[20px]">
                        <MusicArtwork
                            imageUrl={currentItem.imageUrl}
                            seed={`${currentItem.sourcePlatform}-${currentItem.title}`}
                            label={currentItem.title}
                        />
                    </div>
                    <div className="min-w-0 flex-1">
                        <p className="text-[11px] uppercase tracking-[0.24em] text-hud-text-muted">
                            {currentItem.kind} · {currentItem.sourcePlatform}
                        </p>
                        <h3 className="mt-2 truncate text-lg font-semibold text-hud-text-primary">
                            {currentItem.title}
                        </h3>
                        <p className="mt-1 truncate text-sm text-hud-text-secondary">{currentItem.subtitle}</p>
                        {currentItem.supportingText && (
                            <p className="mt-2 line-clamp-2 text-xs leading-5 text-hud-text-muted">
                                {currentItem.supportingText}
                            </p>
                        )}
                    </div>
                    <Button type="button" variant="ghost" onClick={clearItem}>
                        <X size={18} />
                        Close
                    </Button>
                </div>

                <div className="overflow-hidden rounded-[24px] border border-hud-border-secondary bg-hud-bg-primary/85 p-3">
                    {spotifyEmbedUrl ? (
                        <iframe
                            title={`Playback for ${currentItem.title}`}
                            src={spotifyEmbedUrl}
                            width="100%"
                            height={currentItem.kind === 'playlist' ? '352' : '152'}
                            allow="autoplay; clipboard-write; encrypted-media; fullscreen; picture-in-picture"
                            loading="lazy"
                            className="rounded-[18px] border-0"
                        />
                    ) : hasAudioPreview ? (
                        <div className="space-y-3 p-3">
                            <p className="text-sm text-hud-text-secondary">
                                Inline preview is available for this track.
                            </p>
                            <audio
                                key={currentItem.previewUrl}
                                controls
                                autoPlay
                                src={currentItem.previewUrl ?? undefined}
                                className="w-full"
                            />
                        </div>
                    ) : (
                        <div className="flex h-full min-h-[152px] flex-col items-start justify-center gap-4 rounded-[18px] border border-dashed border-hud-border-secondary bg-hud-bg-primary/75 p-5">
                            <p className="text-sm leading-6 text-hud-text-secondary">
                                This item does not have an inline web player yet. Open it in the connected platform to
                                keep the listening flow moving.
                            </p>
                            {currentItem.externalUrl && (
                                <Button
                                    type="button"
                                    variant="primary"
                                    onClick={() => window.open(currentItem.externalUrl ?? undefined, '_blank', 'noopener,noreferrer')}
                                >
                                    <ExternalLink size={18} />
                                    Open in Platform
                                </Button>
                            )}
                        </div>
                    )}
                </div>
            </div>
        </div>
    )
}

export default PlaybackDock
