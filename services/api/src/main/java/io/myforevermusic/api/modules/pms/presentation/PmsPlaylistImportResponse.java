package io.myforevermusic.api.modules.pms.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PmsPlaylistImportResponse(
    String service,
    String status,
    Instant processedAt,
    ImportResult importResult,
    List<ImportedPlaylistResult> playlists,
    NextStep nextStep
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ImportResult(
        String userId,
        String platformId,
        String platformDisplayName,
        int importedPlaylistCount,
        int importedTrackCount,
        int completeSpotifyAudioFeatureTrackCount,
        String connectionMode,
        int librarySyncedPlaylistCount,
        int librarySyncedTrackCount
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ImportedPlaylistResult(
        String playlistId,
        String externalPlaylistId,
        String title,
        String sourcePlatform,
        int trackCount,
        Instant importedAt
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record NextStep(
        String path,
        String message
    ) {
    }
}
