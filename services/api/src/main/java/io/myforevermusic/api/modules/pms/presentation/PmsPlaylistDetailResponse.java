package io.myforevermusic.api.modules.pms.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PmsPlaylistDetailResponse(
    String service,
    String status,
    Instant generatedAt,
    String sourceCollection,
    PlaylistDetail playlist,
    List<TrackDetail> tracks
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PlaylistDetail(
        String playlistId,
        String externalPlaylistId,
        String title,
        String sourcePlatform,
        Integer trackCount,
        String curator,
        String description,
        String coverImageUrl,
        String platformExternalUrl,
        String platformUri,
        Instant importedAt,
        Instant lastSyncedAt
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record TrackDetail(
        String trackId,
        String externalTrackId,
        String title,
        String artistName,
        String sourcePlatform,
        String primaryGenre,
        String albumTitle,
        String albumImageUrl,
        String platformExternalUrl,
        String platformUri,
        String previewUrl,
        Integer durationMs,
        Integer sortOrder,
        Boolean seed,
        String isrc,
        String spotifyTrackId,
        String spotifyUri,
        String tidalTrackId,
        String tidalUri,
        String preferredPlaybackPlatform,
        String playbackTargetStatus,
        String audioFeatureTrackId,
        Boolean spotifyAudioFeaturesFilled,
        Boolean audioFeaturesFilled,
        String spotifyAudioFeatureSource,
        String audioFeatureSource
    ) {
    }
}
