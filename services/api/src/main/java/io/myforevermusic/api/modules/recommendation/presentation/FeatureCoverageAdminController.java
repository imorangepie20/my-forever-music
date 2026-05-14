package io.myforevermusic.api.modules.recommendation.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.myforevermusic.api.modules.recommendation.application.DriftSignalEvaluator;
import io.myforevermusic.api.modules.recommendation.application.FeatureCoverageAdminService;
import io.myforevermusic.api.modules.recommendation.application.FeatureCoverageAdminService.EmsPoolCoverage;
import io.myforevermusic.api.modules.recommendation.application.FeatureCoverageAdminService.EmsSourceCoverage;
import io.myforevermusic.api.modules.recommendation.application.FeatureCoverageAdminService.LearningDataCoverage;
import io.myforevermusic.api.modules.recommendation.application.FeatureCoverageAdminService.PmsLibraryCoverage;
import io.swagger.v3.oas.annotations.Operation;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recommendations/admin/feature-coverage")
public class FeatureCoverageAdminController {

    private final FeatureCoverageAdminService adminService;

    public FeatureCoverageAdminController(FeatureCoverageAdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(summary = "Summarize PMS, EMS, and learning-data feature coverage for recommendation readiness")
    @GetMapping
    public FeatureCoverageAdminResponse getFeatureCoverage(
        @RequestParam("user_id") String userId,
        @RequestParam(value = "target_user_id", required = false) String targetUserId
    ) {
        return FeatureCoverageAdminResponse.from(adminService.summarize(userId, targetUserId));
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record FeatureCoverageAdminResponse(
        String service,
        String status,
        Instant generatedAt,
        String targetUserId,
        PmsLibraryCoverageItem pmsLibrary,
        EmsPoolCoverageItem emsPool,
        LearningDataCoverageItem learningData,
        List<String> warnings,
        List<DriftSignalItem> driftSignals
    ) {
        static FeatureCoverageAdminResponse from(FeatureCoverageAdminService.FeatureCoverageReport report) {
            return new FeatureCoverageAdminResponse(
                "api",
                report.status(),
                report.generatedAt(),
                report.targetUserId(),
                PmsLibraryCoverageItem.from(report.pmsLibrary()),
                EmsPoolCoverageItem.from(report.emsPool()),
                LearningDataCoverageItem.from(report.learningData()),
                report.warnings(),
                report.driftSignals() == null
                    ? List.of()
                    : report.driftSignals().stream().map(DriftSignalItem::from).toList()
            );
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record DriftSignalItem(
        String category,
        String severity,
        String targetScope,
        String message,
        Double actualValue,
        Double threshold,
        long sampleSize
    ) {
        static DriftSignalItem from(DriftSignalEvaluator.DriftSignal signal) {
            return new DriftSignalItem(
                signal.category(),
                signal.severity(),
                signal.targetScope(),
                signal.message(),
                signal.actualValue(),
                signal.threshold(),
                signal.sampleSize()
            );
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PmsLibraryCoverageItem(
        int playlistCount,
        long trackCount,
        long audioFeatureFilledCount,
        double audioFeatureCoverageRatio,
        long isrcCount,
        double isrcCoverageRatio,
        long playbackTargetAvailableCount,
        double playbackTargetCoverageRatio
    ) {
        static PmsLibraryCoverageItem from(PmsLibraryCoverage coverage) {
            return new PmsLibraryCoverageItem(
                coverage.playlistCount(),
                coverage.trackCount(),
                coverage.audioFeatureFilledCount(),
                coverage.audioFeatureCoverageRatio(),
                coverage.isrcCount(),
                coverage.isrcCoverageRatio(),
                coverage.playbackTargetAvailableCount(),
                coverage.playbackTargetCoverageRatio()
            );
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EmsPoolCoverageItem(
        long trackCount,
        long audioFeatureFilledCount,
        double audioFeatureCoverageRatio,
        long isrcCount,
        double isrcCoverageRatio,
        long canonicalTrackCount,
        double canonicalTrackCoverageRatio,
        List<EmsSourceCoverageItem> sources,
        List<String> warnings
    ) {
        static EmsPoolCoverageItem from(EmsPoolCoverage coverage) {
            return new EmsPoolCoverageItem(
                coverage.trackCount(),
                coverage.audioFeatureFilledCount(),
                coverage.audioFeatureCoverageRatio(),
                coverage.isrcCount(),
                coverage.isrcCoverageRatio(),
                coverage.canonicalTrackCount(),
                coverage.canonicalTrackCoverageRatio(),
                coverage.sources().stream().map(EmsSourceCoverageItem::from).toList(),
                coverage.warnings()
            );
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EmsSourceCoverageItem(
        String sourcePlatform,
        long trackCount,
        long audioFeatureFilledCount,
        double audioFeatureCoverageRatio,
        long isrcCount,
        double isrcCoverageRatio,
        long canonicalTrackCount,
        double canonicalTrackCoverageRatio
    ) {
        static EmsSourceCoverageItem from(EmsSourceCoverage coverage) {
            return new EmsSourceCoverageItem(
                coverage.sourcePlatform(),
                coverage.trackCount(),
                coverage.audioFeatureFilledCount(),
                coverage.audioFeatureCoverageRatio(),
                coverage.isrcCount(),
                coverage.isrcCoverageRatio(),
                coverage.canonicalTrackCount(),
                coverage.canonicalTrackCoverageRatio()
            );
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record LearningDataCoverageItem(
        long eventCount,
        long recentRecommendationSnapshotCount,
        int recentRecommendationSnapshotLimit
    ) {
        static LearningDataCoverageItem from(LearningDataCoverage coverage) {
            return new LearningDataCoverageItem(
                coverage.eventCount(),
                coverage.recentRecommendationSnapshotCount(),
                coverage.recentRecommendationSnapshotLimit()
            );
        }
    }
}
