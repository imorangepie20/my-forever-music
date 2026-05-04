package io.myforevermusic.api.modules.gms.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GmsRecommendationPreviewResponse(
    String requestId,
    Instant generatedAt,
    String service,
    String status,
    RecommendationContext context,
    RecommendationInputSummary inputSummary,
    List<RecommendationItem> items,
    List<String> warnings
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record RecommendationContext(
        String strategy,
        String engine,
        String mode,
        String mood,
        Integer energyLevel,
        List<String> seedBasis
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record RecommendationInputSummary(
        String userId,
        String playlistId,
        Integer trackSeedCount,
        Integer artistSeedCount,
        Integer genreSeedCount,
        Integer familiarityBias,
        Integer limit
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record RecommendationItem(
        Integer rank,
        String trackId,
        String title,
        String artistName,
        String sourcePlatform,
        String sourcePlaylistId,
        String sourcePlaylistTitle,
        String albumTitle,
        String albumImageUrl,
        String platformExternalUrl,
        String platformUri,
        String previewUrl,
        String spotifyTrackId,
        Integer durationMs,
        Double score,
        String sourceSpace,
        Integer energyLevel,
        String reason
    ) {
    }
}
