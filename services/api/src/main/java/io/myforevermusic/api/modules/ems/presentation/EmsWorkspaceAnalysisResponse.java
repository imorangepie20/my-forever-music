package io.myforevermusic.api.modules.ems.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record EmsWorkspaceAnalysisResponse(
    String service,
    String status,
    Instant generatedAt,
    AnalysisContext context,
    WorkspaceRecommendation workspaceRecommendation,
    List<SignalCard> topSignals,
    List<String> notes,
    List<String> warnings
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AnalysisContext(
        String strategy,
        String playlistId,
        Integer trackSeedCount,
        Integer artistSeedCount,
        Integer genreSeedCount,
        Integer matchedCatalogTrackCount
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record WorkspaceRecommendation(
        String mood,
        Integer energyLevel,
        Integer familiarityBias,
        Double confidenceScore
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SignalCard(
        String type,
        String label,
        Double weight,
        String reason
    ) {
    }
}
