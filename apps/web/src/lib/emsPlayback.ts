import {
    extractSpotifyTrackIdFromUrl,
    extractTidalTrackIdFromUrl,
    type PlaybackMediaItem,
} from '@/lib/musicPlayback'
import type { EmsCollectionTrackItem } from '@/types/api'

export const buildEmsPlaylistDetailPath = (playlistId: number) =>
    `/playlists/ems/${encodeURIComponent(String(playlistId))}`

export const platformUriFor = (item: { platform_uri?: string | null; spotify_uri?: string | null }) =>
    item.platform_uri ?? item.spotify_uri ?? null

export const toEmsTrackPlaybackItem = (
    track: EmsCollectionTrackItem,
    playlistTitle?: string | null,
): PlaybackMediaItem => {
    const platformUri = platformUriFor(track)
    const spotifyTrackId =
        track.source_platform === 'spotify'
            ? extractSpotifyTrackIdFromUrl(track.external_track_id) ?? extractSpotifyTrackIdFromUrl(platformUri)
            : null
    const tidalTrackId =
        track.source_platform === 'tidal'
            ? extractTidalTrackIdFromUrl(track.external_track_id) ?? extractTidalTrackIdFromUrl(platformUri)
            : null

    return {
        id: `ems-track:${track.id}`,
        kind: 'track',
        title: track.title,
        subtitle: `${track.artist_name} · ${track.source_platform}`,
        sourcePlatform: track.source_platform,
        playbackPlatformId: null,
        externalTrackId: track.external_track_id,
        imageUrl: track.album_image_url,
        albumTitle: track.album_title,
        externalUrl: track.platform_external_url,
        platformUri,
        previewUrl: track.preview_url,
        spotifyTrackId,
        tidalTrackId,
        isrc: track.isrc,
        durationMs: track.duration_ms,
        supportingText: playlistTitle ?? null,
    }
}
