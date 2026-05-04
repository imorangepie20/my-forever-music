export type PlaybackKind = 'track' | 'playlist'

export interface PlaybackMediaItem {
    id: string
    kind: PlaybackKind
    title: string
    subtitle: string
    sourcePlatform: string
    imageUrl?: string | null
    albumTitle?: string | null
    externalUrl?: string | null
    platformUri?: string | null
    previewUrl?: string | null
    spotifyTrackId?: string | null
    durationMs?: number | null
    supportingText?: string | null
}

const SPOTIFY_EMBED_BASE = 'https://open.spotify.com/embed'

const readSpotifyIdFromUri = (platformUri?: string | null) => {
    if (!platformUri || !platformUri.startsWith('spotify:')) {
        return null
    }

    const [, resourceType, resourceId] = platformUri.split(':')
    if (!resourceType || !resourceId) {
        return null
    }

    return { resourceType, resourceId }
}

const readSpotifyIdFromUrl = (externalUrl?: string | null) => {
    if (!externalUrl) {
        return null
    }

    const match = externalUrl.match(/open\.spotify\.com\/(track|playlist|album)\/([^?]+)/)
    if (!match) {
        return null
    }

    return {
        resourceType: match[1],
        resourceId: match[2],
    }
}

export const resolveSpotifyEmbedUrl = (item: PlaybackMediaItem) => {
    const fromUri = readSpotifyIdFromUri(item.platformUri)
    if (fromUri) {
        return `${SPOTIFY_EMBED_BASE}/${fromUri.resourceType}/${fromUri.resourceId}?utm_source=my-forever-music`
    }

    const fromUrl = readSpotifyIdFromUrl(item.externalUrl)
    if (fromUrl) {
        return `${SPOTIFY_EMBED_BASE}/${fromUrl.resourceType}/${fromUrl.resourceId}?utm_source=my-forever-music`
    }

    if (item.kind === 'track' && item.spotifyTrackId) {
        return `${SPOTIFY_EMBED_BASE}/track/${item.spotifyTrackId}?utm_source=my-forever-music`
    }

    return null
}

export const formatDuration = (durationMs?: number | null) => {
    if (!durationMs || durationMs <= 0) {
        return null
    }

    const totalSeconds = Math.round(durationMs / 1000)
    const minutes = Math.floor(totalSeconds / 60)
    const seconds = totalSeconds % 60
    return `${minutes}:${String(seconds).padStart(2, '0')}`
}
