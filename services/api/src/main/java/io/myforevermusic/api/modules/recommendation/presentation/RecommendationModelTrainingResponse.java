package io.myforevermusic.api.modules.recommendation.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.myforevermusic.api.modules.recommendation.infrastructure.ai.AiSasrecTrainingClient;
import java.util.List;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RecommendationModelTrainingResponse(
    String service,
    String status,
    String userId,
    DatasetSummary datasetSummary,
    String modelVersion,
    Map<String, Object> trainingSummary,
    Map<String, Object> metrics,
    Map<String, Object> baselineMetrics,
    Map<String, Object> metricDelta,
    Map<String, Object> qualification,
    Map<String, Object> modelArtifact,
    List<String> warnings
) {

    public static RecommendationModelTrainingResponse from(
        RecommendationDatasetExportResponse dataset,
        AiSasrecTrainingClient.SasrecTrainingResponse trainingResponse
    ) {
        return new RecommendationModelTrainingResponse(
            trainingResponse.service(),
            trainingResponse.status(),
            trainingResponse.userId(),
            new DatasetSummary(
                dataset.eventLimit(),
                dataset.snapshotLimit(),
                dataset.summary().eventCount(),
                dataset.summary().recommendationSnapshotCount(),
                dataset.summary().sequenceItemCount(),
                dataset.summary().datasetVersion(),
                dataset.summary().datasetFingerprint()
            ),
            trainingResponse.modelVersion(),
            trainingResponse.summary(),
            trainingResponse.metrics(),
            trainingResponse.baselineMetrics(),
            trainingResponse.metricDelta(),
            trainingResponse.qualification(),
            trainingResponse.modelArtifact(),
            trainingResponse.warnings()
        );
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record DatasetSummary(
        Integer eventLimit,
        Integer snapshotLimit,
        Integer eventCount,
        Integer recommendationSnapshotCount,
        Integer sequenceItemCount,
        String datasetVersion,
        String datasetFingerprint
    ) {
    }
}
