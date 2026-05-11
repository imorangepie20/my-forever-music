package io.myforevermusic.api.modules.recommendation.presentation;

import io.myforevermusic.api.modules.recommendation.application.RecommendationDatasetExportService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recommendations/datasets")
public class RecommendationDatasetExportController {

    private final RecommendationDatasetExportService datasetExportService;

    public RecommendationDatasetExportController(RecommendationDatasetExportService datasetExportService) {
        this.datasetExportService = datasetExportService;
    }

    @Operation(summary = "Export user music events and recommendation snapshots as a model training sequence")
    @GetMapping("/users/{userId}/sequence")
    public RecommendationDatasetExportResponse exportUserSequence(
        @PathVariable String userId,
        @RequestParam(name = "event_limit", required = false) Integer eventLimit,
        @RequestParam(name = "snapshot_limit", required = false) Integer snapshotLimit
    ) {
        return datasetExportService.exportUserSequence(userId, eventLimit, snapshotLimit);
    }
}
