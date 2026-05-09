export type PlaybackKind = 'track' | 'playlist'

export interface PlaybackMediaItem {
    id: string
    kind: PlaybackKind
    title: string
    subtitle: string
    sourcePlatform: string
    playbackPlatformId?: string | null
    imageUrl?: string | null
    albumTitle?: string | null
    externalUrl?: string | null
    platformUri?: string | null
    previewUrl?: string | null
    spotifyTrackId?: string | null
    tidalTrackId?: string | null
    durationMs?: number | null
    supportingText?: string | null
}

const SPOTIFY_EMBED_BASE = 'https://open.spotify.com/embed'
const SPOTIFY_TRACK_ID_PATTERN = /^[A-Za-z0-9]{22}$/
const TIDAL_TRACK_ID_PATTERN = /^[A-Za-z0-9-]{2,80}$/

const readSpotifyIdFromUri = (platformUri?: string | null) => {
    if (!platformUri || !platformUri.startsWith('spotify:')) {
        return null
    }

    const [, resourceType, resourceId] = platformUri.split(':')
    if (!resourceType || !resourceId) {
        return null
    }

    return { resourceType, resourceId: normalizeSpotifyTrackId(resourceId) ?? resourceId }
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
        resourceId: normalizeSpotifyTrackId(match[2]) ?? match[2],
    }
}

export const normalizeSpotifyTrackId = (value?: string | null) => {
    if (!value) {
        return null
    }

    const trimmed = value.trim()
    if (SPOTIFY_TRACK_ID_PATTERN.test(trimmed)) {
        return trimmed
    }

    return null
}

export const extractSpotifyTrackIdFromUrl = (value?: string | null) => {
    if (!value) {
        return null
    }

    const trimmed = value.trim()
    const uriMatch = trimmed.match(/spotify:track:([A-Za-z0-9]{22})/)
    if (uriMatch) {
        return uriMatch[1]
    }

    const urlMatch = trimmed.match(/open\.spotify\.com\/track\/([A-Za-z0-9]{22})/)
    if (urlMatch) {
        return urlMatch[1]
    }

    return normalizeSpotifyTrackId(trimmed)
}

export const normalizeTidalTrackId = (value?: string | null) => {
    if (!value) {
        return null
    }

    const trimmed = value.trim()
    if (!trimmed || trimmed.includes(':') || trimmed.includes('/') || /\s/.test(trimmed)) {
        return null
    }

    if (TIDAL_TRACK_ID_PATTERN.test(trimmed)) {
        return trimmed
    }

    return null
}

export const extractTidalTrackIdFromUrl = (value?: string | null) => {
    if (!value) {
        return null
    }

    const trimmed = value.trim()
    const uriMatch = trimmed.match(/^tidal:track:([^:/?#\s]+)$/i)
    if (uriMatch) {
        return normalizeTidalTrackId(uriMatch[1])
    }

    const urlMatch = trimmed.match(/tidal\.com\/(?:browse\/)?track\/([^?/#\s]+)/i)
    if (urlMatch) {
        return normalizeTidalTrackId(urlMatch[1])
    }

    return normalizeTidalTrackId(trimmed)
}

export const resolveSpotifyTrackId = (item: PlaybackMediaItem) =>
    normalizeSpotifyTrackId(item.spotifyTrackId) ??
    extractSpotifyTrackIdFromUrl(item.platformUri) ??
    extractSpotifyTrackIdFromUrl(item.externalUrl)

export const resolveTidalTrackId = (item: PlaybackMediaItem) =>
    normalizeTidalTrackId(item.tidalTrackId) ??
    extractTidalTrackIdFromUrl(item.platformUri) ??
    extractTidalTrackIdFromUrl(item.externalUrl)

export const resolveSpotifyContextUri = (item: PlaybackMediaItem) => {
    const fromUri = readSpotifyIdFromUri(item.platformUri)
    if (fromUri && ['playlist', 'album'].includes(fromUri.resourceType)) {
        return `spotify:${fromUri.resourceType}:${fromUri.resourceId}`
    }

    const fromUrl = readSpotifyIdFromUrl(item.externalUrl)
    if (fromUrl && ['playlist', 'album'].includes(fromUrl.resourceType)) {
        return `spotify:${fromUrl.resourceType}:${fromUrl.resourceId}`
    }

    return null
}

export const resolvePlaybackPlatformId = (item: PlaybackMediaItem, fallbackPlatformId?: string | null) => {
    if (item.playbackPlatformId) {
        return item.playbackPlatformId
    }

    if (item.sourcePlatform === 'tidal' && resolveTidalTrackId(item)) {
        return 'tidal'
    }

    if (item.sourcePlatform === 'spotify' && resolveSpotifyTrackId(item)) {
        return 'spotify'
    }

    return (
        (resolveSpotifyTrackId(item) ? 'spotify' : null) ??
        (resolveTidalTrackId(item) ? 'tidal' : null) ??
        item.sourcePlatform ??
        fallbackPlatformId ??
        null
    )
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

    const spotifyTrackId = resolveSpotifyTrackId(item)
    if (item.kind === 'track' && spotifyTrackId) {
        return `${SPOTIFY_EMBED_BASE}/track/${spotifyTrackId}?utm_source=my-forever-music`
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
