package io.myforevermusic.api.modules.recommendation.application;

import io.myforevermusic.api.modules.recommendation.infrastructure.ai.AiSasrecRegistryClient.SasrecRegistryResponse;
import io.myforevermusic.api.modules.recommendation.infrastructure.ai.AiSasrecTrainingClient;
import io.myforevermusic.api.modules.recommendation.presentation.RecommendationModelTrainingResponse;
import io.myforevermusic.api.modules.recommendation.presentation.RecommendationDatasetExportResponse;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RecommendationModelTrainingService {

    private final RecommendationDatasetExportService datasetExportService;
    private final AiSasrecTrainingClient aiSasrecTrainingClient;
    private final SasrecModelRegistryAdminService registryAdminService;

    public RecommendationModelTrainingService(
        RecommendationDatasetExportService datasetExportService,
        AiSasrecTrainingClient aiSasrecTrainingClient,
        SasrecModelRegistryAdminService registryAdminService
    ) {
        this.datasetExportService = datasetExportService;
        this.aiSasrecTrainingClient = aiSasrecTrainingClient;
        this.registryAdminService = registryAdminService;
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

    public AutoTrainResult autoTrainAndPromote(
        String userId,
        Integer eventLimit,
        Integer snapshotLimit,
        AiSasrecTrainingClient.SasrecTrainingOptions trainingOptions
    ) {
        RecommendationModelTrainingResponse training = trainSasrecModel(
            userId,
            eventLimit,
            snapshotLimit,
            trainingOptions
        );

        boolean qualified = isQualified(training.qualification());
        SasrecRegistryResponse promoteResult = null;
        String summary;
        if (!qualified) {
            summary = "qualification=false — auto-promote skipped";
        } else if (training.modelVersion() == null || training.modelVersion().isBlank()) {
            summary = "qualified=true 이지만 model_version 이 비어 있어 promote 를 건너뛰었습니다.";
        } else if (!isArtifactSaved(training.modelArtifact())) {
            summary = "qualified=true 이지만 artifact 가 저장되지 않아 promote 를 건너뛰었습니다.";
        } else {
            promoteResult = registryAdminService.promote(userId, training.modelVersion());
            summary = "qualified=true — %s 를 active model 로 promote 했습니다.".formatted(training.modelVersion());
        }

        return new AutoTrainResult(training, qualified, promoteResult, summary);
    }

    private boolean isQualified(Map<String, Object> qualification) {
        if (qualification == null) {
            return false;
        }
        Object qualified = qualification.get("qualified");
        return Boolean.TRUE.equals(qualified);
    }

    private boolean isArtifactSaved(Map<String, Object> modelArtifact) {
        if (modelArtifact == null) {
            return false;
        }
        return Boolean.TRUE.equals(modelArtifact.get("saved"));
    }

    public record AutoTrainResult(
        RecommendationModelTrainingResponse training,
        boolean qualified,
        SasrecRegistryResponse promoteResult,
        String summary
    ) {}
}
