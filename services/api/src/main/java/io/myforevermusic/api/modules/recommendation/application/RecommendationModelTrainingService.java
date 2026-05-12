package io.myforevermusic.api.modules.recommendation.application;

import io.myforevermusic.api.modules.recommendation.infrastructure.ai.AiSasrecTrainingClient;
import io.myforevermusic.api.modules.recommendation.presentation.RecommendationModelTrainingResponse;
import io.myforevermusic.api.modules.recommendation.presentation.RecommendationDatasetExportResponse;
import org.springframework.stereotype.Service;

@Service
public class RecommendationModelTrainingService {

    private final RecommendationDatasetExportService datasetExportService;
    private final AiSasrecTrainingClient aiSasrecTrainingClient;

    public RecommendationModelTrainingService(
        RecommendationDatasetExportService datasetExportService,
        AiSasrecTrainingClient aiSasrecTrainingClient
    ) {
        this.datasetExportService = datasetExportService;
        this.aiSasrecTrainingClient = aiSasrecTrainingClient;
    }

    public RecommendationModelTrainingResponse trainSasrecModel(
        String userId,
        Integer eventLimit,
        Integer snapshotLimit,
        AiSasrecTrainingClient.SasrecTrainingOptions trainingOptions
    ) {
        RecommendationDatasetExportResponse dataset = datasetExportService.exportUserSequence(
            userId,
            eventLimit,
            snapshotLimit
        );
        AiSasrecTrainingClient.SasrecTrainingResponse trainingResponse = aiSasrecTrainingClient.train(
            dataset,
            trainingOptions
        );
        return RecommendationModelTrainingResponse.from(dataset, trainingResponse);
    }
}
