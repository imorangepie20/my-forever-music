import {
    extractSpotifyTrackIdFromUrl,
    extractTidalTrackIdFromUrl,
    type PlaybackMediaItem,
} from '@/lib/musicPlayback'
import type {
    PmsPlaylistDetailResponse,
    PmsPlaylistDetailTrack,
} from '@/types/api'

interface PmsPlayablePlaylist {
    playlist_id: string
    title: string
    source_platform: string
    curator?: string | null
    cover_image_url?: string | null
    platform_external_url?: string | null
    platform_uri?: string | null
    highlight?: string | null
    description?: string | null
}

export const buildPmsPlaylistDetailPath = (playlistId: string) =>
    `/playlists/pms/${encodeURIComponent(playlistId)}`

export const resolvePmsTrackSpotifyId = (track: {
    source_platform: string
    external_track_id?: string | null
    spotify_track_id?: string | null
    audio_feature_track_id?: string | null
    spotify_uri?: string | null
    platform_uri?: string | null
    platform_external_url?: string | null
}) => {
    const fromSpotifyUri = extractSpotifyTrackIdFromUrl(track.spotify_uri)
    if (fromSpotifyUri) {
        return fromSpotifyUri
    }

    const fromSpotifyTrackId = extractSpotifyTrackIdFromUrl(track.spotify_track_id)
    if (fromSpotifyTrackId) {
        return fromSpotifyTrackId
    }

    const fromPlatformUri = extractSpotifyTrackIdFromUrl(track.platform_uri)
    if (fromPlatformUri) {
        return fromPlatformUri
    }

    const fromExternalUrl = extractSpotifyTrackIdFromUrl(track.platform_external_url)
    if (fromExternalUrl) {
        return fromExternalUrl
    }

    const fromExternalTrackId = extractSpotifyTrackIdFromUrl(track.external_track_id)
    if (track.source_platform === 'spotify' && fromExternalTrackId) {
        return fromExternalTrackId
    }

    if (track.source_platform === 'spotify') {
        return extractSpotifyTrackIdFromUrl(track.audio_feature_track_id)
    }

    return null
}

export const resolvePmsTrackTidalId = (track: {
    source_platform: string
    external_track_id?: string | null
    tidal_track_id?: string | null
    tidal_uri?: string | null
    platform_uri?: string | null
    platform_external_url?: string | null
}) => {
    const fromTidalUri = extractTidalTrackIdFromUrl(track.tidal_uri)
    if (fromTidalUri) {
        return fromTidalUri
    }

    const fromTidalTrackId = extractTidalTrackIdFromUrl(track.tidal_track_id)
    if (fromTidalTrackId) {
        return fromTidalTrackId
    }

    const nativeTidalTrackId = (
        extractTidalTrackIdFromUrl(track.platform_uri) ??
        extractTidalTrackIdFromUrl(track.platform_external_url) ??
        extractTidalTrackIdFromUrl(track.external_track_id)
    )
    return track.source_platform === 'tidal' ? nativeTidalTrackId : null
}

export const resolvePmsPlaybackPlatformId = (
    sourcePlatform: string,
    spotifyTrackId?: string | null,
    tidalTrackId?: string | null,
) => {
    if (sourcePlatform === 'tidal') {
        return tidalTrackId ? 'tidal' : spotifyTrackId ? 'spotify' : 'tidal'
    }

    if (sourcePlatform === 'spotify') {
        return spotifyTrackId ? 'spotify' : tidalTrackId ? 'tidal' : 'spotify'
    }

    if (spotifyTrackId) {
        return 'spotify'
    }

    if (tidalTrackId) {
        return 'tidal'
    }

    return sourcePlatform || null
}

export const toPmsTrackPlaybackItem = (
    track: PmsPlaylistDetailTrack,
    playlistTitle?: string | null,
): PlaybackMediaItem => {
    const spotifyTrackId = resolvePmsTrackSpotifyId(track)
    const tidalTrackId = resolvePmsTrackTidalId(track)
    const playbackPlatformId =
        track.preferred_playback_platform ??
        resolvePmsPlaybackPlatformId(track.source_platform, spotifyTrackId, tidalTrackId)

    return {
        id: `pms-track:${track.track_id}`,
        kind: 'track',
        title: track.title,
        subtitle: `${track.artist_name} · ${track.source_platform}`,
        sourcePlatform: track.source_platform,
        playbackPlatformId,
        externalTrackId: track.external_track_id,
        imageUrl: track.album_image_url,
        albumTitle: track.album_title,
        externalUrl: track.platform_external_url,
        platformUri: track.platform_uri,
        previewUrl: track.preview_url,
        spotifyTrackId,
        tidalTrackId,
        isrc: track.isrc,
        durationMs: track.duration_ms,
        supportingText: playlistTitle ?? null,
    }
}

export const toPmsPlaylistPlaybackItem = (
    playlist: PmsPlayablePlaylist | PmsPlaylistDetailResponse['playlist'],
): PlaybackMediaItem => ({
    id: `pms-playlist:${playlist.playlist_id}`,
    kind: 'playlist',
    title: playlist.title,
    subtitle: `${playlist.curator ?? 'PMS library'} · ${playlist.source_platform}`,
    sourcePlatform: playlist.source_platform,
    playbackPlatformId: playlist.source_platform === 'spotify' ? 'spotify' : playlist.source_platform,
    imageUrl: playlist.cover_image_url,
    externalUrl: playlist.platform_external_url,
    platformUri: playlist.platform_uri,
    supportingText: 'highlight' in playlist ? playlist.highlight : playlist.description ?? null,
})
