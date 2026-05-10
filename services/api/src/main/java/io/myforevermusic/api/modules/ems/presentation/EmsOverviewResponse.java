package io.myforevermusic.api.modules.ems.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record EmsOverviewResponse(
    String service,
    String status,
    Instant generatedAt,
    String userId,
    String playlistId,
    PipelineStatus pipelineStatus,
    TasteModelSnapshot tasteModelSnapshot,
    CandidateDirection candidateDirection,
    PmsContext pmsContext,
    EmsPoolHealth emsPool,
    List<String> systemAttention,
    List<String> evidence,
    List<String> warnings
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PipelineStatus(
        String pmsLibrary,
        String emsPool,
        String gmsReadiness
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record TasteModelSnapshot(
        String status,
        String model,
        String summary,
        Double confidence
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CandidateDirection(
        String status,
        String summary,
        String mood,
        Integer energyLevel,
        Integer familiarityBias,
        Double confidence
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PmsContext(
        String playlistTitle,
        Integer playlistCount,
        Integer libraryTrackCount,
        Integer seedTrackCount,
        Integer artistSeedCount,
        Integer genreSeedCount
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EmsPoolHealth(
        Integer playlistCount,
        Integer trackCount,
        Integer audioFeatureFilledTrackCount,
        Double audioFeatureCoverageRatio,
        List<ProviderPool> providers
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ProviderPool(
        String platformId,
        Long playlistCount,
        Long trackCount,
        Long audioFeatureFilledTrackCount,
        Double audioFeatureCoverageRatio,
        Instant lastCollectedAt
    ) {
    }
}
