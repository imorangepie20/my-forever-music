import {
    resolveSpotifyTrackId,
    resolveTidalTrackId,
    type PlaybackMediaItem,
} from '@/lib/musicPlayback'
import { resolveSpotifyPlaybackTarget } from '@/services/api'

export const resolveSpotifyPlayableItem = async (userId: string, item: PlaybackMediaItem): Promise<PlaybackMediaItem> => {
    const spotifyTrackId = resolveSpotifyTrackId(item)
    if (spotifyTrackId) {
        return {
            ...item,
            playbackPlatformId: 'spotify',
            spotifyTrackId,
            platformUri: item.platformUri ?? `spotify:track:${spotifyTrackId}`,
        }
    }

    const target = await resolveSpotifyPlaybackTarget({
        user_id: userId,
        title: item.title,
        artist_name: item.subtitle.split(' · ')[0] || item.subtitle,
        source_platform: item.sourcePlatform,
        external_track_id: item.externalTrackId,
        platform_uri: item.platformUri,
        tidal_track_id: resolveTidalTrackId(item),
        isrc: item.isrc,
        duration_ms: item.durationMs,
    })

    return {
        ...item,
        playbackPlatformId: 'spotify',
        spotifyTrackId: target.spotify_track_id,
        platformUri: target.spotify_uri ?? item.platformUri,
        externalUrl: target.platform_external_url ?? item.externalUrl,
        albumTitle: target.album_title ?? item.albumTitle,
        imageUrl: target.album_image_url ?? item.imageUrl,
        previewUrl: target.preview_url ?? item.previewUrl,
        isrc: target.isrc ?? item.isrc,
        durationMs: target.duration_ms ?? item.durationMs,
    }
}
