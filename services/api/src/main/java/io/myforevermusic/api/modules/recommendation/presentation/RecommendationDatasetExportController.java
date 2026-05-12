package io.myforevermusic.api.modules.recommendation.presentation;

import io.myforevermusic.api.modules.recommendation.application.RecommendationDatasetExportService;
import io.myforevermusic.api.modules.recommendation.application.RecommendationModelTrainingService;
import io.myforevermusic.api.modules.recommendation.infrastructure.ai.AiSasrecTrainingClient;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recommendations/datasets")
public class RecommendationDatasetExportController {

    private final RecommendationDatasetExportService datasetExportService;
    private final RecommendationModelTrainingService modelTrainingService;

    public RecommendationDatasetExportController(
        RecommendationDatasetExportService datasetExportService,
        RecommendationModelTrainingService modelTrainingService
    ) {
        this.datasetExportService = datasetExportService;
        this.modelTrainingService = modelTrainingService;
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

    @Operation(summary = "Export a user's recommendation sequence and train a persisted SASRec MVP model")
    @PostMapping("/users/{userId}/sasrec/train")
    public RecommendationModelTrainingResponse trainUserSasrecModel(
        @PathVariable String userId,
        @RequestParam(name = "event_limit", required = false) Integer eventLimit,
        @RequestParam(name = "snapshot_limit", required = false) Integer snapshotLimit,
        @RequestParam(name = "max_context_length", required = false, defaultValue = "50") Integer maxContextLength,
        @RequestParam(name = "k", required = false, defaultValue = "10") Integer k,
        @RequestParam(name = "epochs", required = false, defaultValue = "30") Integer epochs,
        @RequestParam(name = "hidden_size", required = false, defaultValue = "32") Integer hiddenSize,
        @RequestParam(name = "learning_rate", required = false, defaultValue = "0.01") Double learningRate,
        @RequestParam(name = "persist_artifact", required = false, defaultValue = "true") Boolean persistArtifact
    ) {
        return modelTrainingService.trainSasrecModel(
            userId,
            eventLimit,
            snapshotLimit,
            new AiSasrecTrainingClient.SasrecTrainingOptions(
                maxContextLength,
                k,
                epochs,
                hiddenSize,
                learningRate,
                persistArtifact
            )
        );
    }
}
