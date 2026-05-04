package io.myforevermusic.api.modules.pms.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PmsWorkspaceBootstrapResponse(
    String service,
    String status,
    Instant generatedAt,
    WorkspaceDefaults workspaceDefaults,
    List<PlaylistOption> playlists,
    List<TrackSeedSuggestion> suggestedTracks,
    List<ArtistSeedSuggestion> suggestedArtists,
    List<GenreSeedSuggestion> suggestedGenres
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record WorkspaceDefaults(
        String userId,
        String playlistId,
        List<String> seedTrackIds,
        List<String> seedArtistNames,
        List<String> seedGenres
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PlaylistOption(
        String playlistId,
        String title,
        String sourcePlatform,
        Integer trackCount,
        String curator,
        String highlight,
        String coverImageUrl,
        String platformExternalUrl,
        String platformUri
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record TrackSeedSuggestion(
        String trackId,
        String title,
        String artistName,
        String sourcePlatform,
        String albumTitle,
        String albumImageUrl,
        String platformExternalUrl,
        String platformUri,
        String previewUrl,
        Integer durationMs,
        boolean seed,
        String spotifyTrackId,
        boolean spotifyAudioFeaturesFilled,
        String spotifyAudioFeatureSource
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ArtistSeedSuggestion(
        String artistName,
        Double affinityScore,
        String reason
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record GenreSeedSuggestion(
        String genre,
        Double weight,
        String reason
    ) {
    }
}
