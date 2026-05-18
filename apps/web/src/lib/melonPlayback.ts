import type { PlaybackMediaItem } from '@/lib/musicPlayback'
import type { MelonChartTrack } from '@/types/api'

export const toMelonHot100PlaybackItem = (track: MelonChartTrack): PlaybackMediaItem => ({
    id: `melon-hot-100:${track.melon_song_id ?? track.rank}`,
    kind: 'track',
    title: track.title,
    subtitle: `${track.artist_name} · Melon Hot 100 #${track.rank}`,
    sourcePlatform: 'melon',
    playbackPlatformId: null,
    externalTrackId: track.melon_song_id ?? `rank-${track.rank}`,
    imageUrl: track.image_url,
    albumTitle: track.album_title,
    externalUrl: track.song_external_url,
    supportingText: 'Melon Hot 100',
})
