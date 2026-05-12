package io.myforevermusic.api.modules.recommendation.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.myforevermusic.api.modules.recommendation.application.RecommendationSnapshotService;
import io.myforevermusic.api.modules.recommendation.application.RecommendationSnapshotService.PlaylistQualityAdminSummary;
import io.swagger.v3.oas.annotations.Operation;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recommendations/admin/playlist-quality")
public class RecommendationPlaylistQualityAdminController {

    private final RecommendationSnapshotService snapshotService;

    public RecommendationPlaylistQualityAdminController(RecommendationSnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @Operation(summary = "List recent playlist-level quality summaries for the configured admin user")
    @GetMapping("/recent")
    public RecommendationPlaylistQualityRecentResponse listRecent(
        @RequestParam("user_id") String userId,
        @RequestParam(value = "limit", defaultValue = "10") int limit
    ) {
        List<PlaylistQualityAdminSummary> summaries = snapshotService.summarizeRecentForAdmin(userId, limit);
        return new RecommendationPlaylistQualityRecentResponse(
            "api",
            "ok",
            Instant.now(),
            summaries.stream().map(PlaylistQualityRecentItem::from).toList()
        );
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record RecommendationPlaylistQualityRecentResponse(
        String service,
        String status,
        Instant generatedAt,
        List<PlaylistQualityRecentItem> playlists
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PlaylistQualityRecentItem(
        String recommendationId,
        String userId,
        Instant createdAt,
        String modelVersion,
        int trackCount,
        Double avgAffinity,
        Double avgNovelty,
        Double coherence,
        Double diversity,
        Double redundancyPenalty,
        Double avgConfidence
    ) {
        static PlaylistQualityRecentItem from(PlaylistQualityAdminSummary summary) {
            return new PlaylistQualityRecentItem(
                summary.recommendationId(),
                summary.userId(),
                summary.createdAt(),
                summary.modelVersion(),
                summary.trackCount(),
                summary.avgAffinity(),
                summary.avgNovelty(),
                summary.coherence(),
                summary.diversity(),
                summary.redundancyPenalty(),
                summary.avgConfidence()
            );
        }
    }
}
