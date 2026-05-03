package io.myforevermusic.api.modules.pms.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PmsPlaylistImportBootstrapResponse(
    String service,
    String status,
    Instant generatedAt,
    ImportUser user,
    PreferredPlatformConnection platformConnection,
    ImportSummary summary,
    List<AvailablePlaylist> availablePlaylists,
    List<ImportedPlaylist> importedPlaylists
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ImportUser(
        String userId,
        String displayName,
        String preferredPlatformId
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PreferredPlatformConnection(
        String platformId,
        String displayName,
        boolean connected,
        String connectionMode,
        String externalAccountLabel,
        boolean syncReady
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ImportSummary(
        boolean preferredPlatformConnected,
        int availablePlaylistCount,
        int importedPlaylistCount,
        String nextStepPath,
        String nextStepMessage
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AvailablePlaylist(
        String externalPlaylistId,
        String title,
        String sourcePlatform,
        int trackCount,
        String curator,
        String description,
        boolean alreadyImported,
        String audioFeaturePolicy
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ImportedPlaylist(
        String playlistId,
        String externalPlaylistId,
        String title,
        String sourcePlatform,
        int trackCount,
        Instant importedAt
    ) {
    }
}
