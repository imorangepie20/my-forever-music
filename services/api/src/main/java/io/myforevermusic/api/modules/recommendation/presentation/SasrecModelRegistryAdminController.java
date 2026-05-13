package io.myforevermusic.api.modules.recommendation.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.myforevermusic.api.modules.recommendation.application.RecommendationModelTrainingService;
import io.myforevermusic.api.modules.recommendation.application.SasrecModelRegistryAdminService;
import io.myforevermusic.api.modules.recommendation.infrastructure.ai.AiSasrecRegistryClient.SasrecRegistryResponse;
import io.myforevermusic.api.modules.recommendation.infrastructure.ai.AiSasrecTrainingClient;
import io.swagger.v3.oas.annotations.Operation;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recommendations/admin/sasrec/models")
public class SasrecModelRegistryAdminController {

    private final SasrecModelRegistryAdminService adminService;

    public SasrecModelRegistryAdminController(SasrecModelRegistryAdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(summary = "Resolve the active SASRec model for the configured admin user")
    @GetMapping("/latest")
    public SasrecRegistryAdminResponse getLatest(@RequestParam("user_id") String userId) {
        return SasrecRegistryAdminResponse.from(adminService.latest(userId));
    }

    @Operation(summary = "Promote a SASRec model version as active for the configured admin user")
    @PostMapping("/{modelVersion}/promote")
    public SasrecRegistryAdminResponse promote(
        @PathVariable String modelVersion,
        @RequestParam("user_id") String userId
    ) {
        return SasrecRegistryAdminResponse.from(adminService.promote(userId, modelVersion));
    }

    @Operation(summary = "Disable a SASRec model version from being served")
    @PostMapping("/{modelVersion}/disable")
    public SasrecRegistryAdminResponse disable(
        @PathVariable String modelVersion,
        @RequestParam("user_id") String userId
    ) {
        return SasrecRegistryAdminResponse.from(adminService.disable(userId, modelVersion));
    }

    @Operation(summary = "Roll back the active SASRec model to the previously promoted version")
    @PostMapping("/rollback")
    public SasrecRegistryAdminResponse rollback(@RequestParam("user_id") String userId) {
        return SasrecRegistryAdminResponse.from(adminService.rollback(userId));
    }

    @Operation(summary = "Resolve another user's model stage for admin debugging")
    @GetMapping("/users/{targetUserId}/status")
    public UserModelStatusResponse getUserStatus(
        @PathVariable String targetUserId,
        @RequestParam("user_id") String userId
    ) {
        SasrecModelRegistryAdminService.UserModelStatus status = adminService.getUserModelStatus(userId, targetUserId);
        return UserModelStatusResponse.from(status);
    }

    @Operation(summary = "Run a SASRec training pass and auto-promote when qualified")
    @PostMapping("/auto-train")
    public SasrecAutoTrainAdminResponse autoTrain(
        @RequestParam("user_id") String userId,
        @RequestParam(value = "event_limit", required = false) Integer eventLimit,
        @RequestParam(value = "snapshot_limit", required = false) Integer snapshotLimit,
        @RequestParam(value = "max_context_length", defaultValue = "32") int maxContextLength,
        @RequestParam(value = "k", defaultValue = "10") int k,
        @RequestParam(value = "epochs", defaultValue = "30") int epochs,
        @RequestParam(value = "hidden_size", defaultValue = "32") int hiddenSize,
        @RequestParam(value = "learning_rate", defaultValue = "0.01") double learningRate,
        @RequestParam(value = "persist_artifact", defaultValue = "true") boolean persistArtifact
    ) {
        AiSasrecTrainingClient.SasrecTrainingOptions options = new AiSasrecTrainingClient.SasrecTrainingOptions(
            maxContextLength,
            k,
            epochs,
            hiddenSize,
            learningRate,
            persistArtifact
        );
        RecommendationModelTrainingService.AutoTrainResult result = adminService.autoTrainAndPromote(
            userId,
            eventLimit,
            snapshotLimit,
            options
        );
        return SasrecAutoTrainAdminResponse.from(result);
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SasrecRegistryAdminResponse(
        String service,
        String status,
        Instant generatedAt,
        String userId,
        String modelVersion,
        String artifactDir,
        String generatedAtAi,
        Integer vocabularySize,
        Integer trainExampleCount,
        java.util.List<String> warnings
    ) {
        static SasrecRegistryAdminResponse from(SasrecRegistryResponse response) {
            return new SasrecRegistryAdminResponse(
                "api",
                response.status(),
                Instant.now(),
                response.userId(),
                response.modelVersion(),
                response.artifactDir(),
                response.generatedAt(),
                response.vocabularySize(),
                response.trainExampleCount(),
                response.warnings() == null ? java.util.List.of() : response.warnings()
            );
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record UserModelStatusResponse(
        String service,
        String status,
        Instant generatedAt,
        String userId,
        String modelStage,
        long pmsTrackCount,
        String activeModelVersion,
        String activeModelGeneratedAt,
        TrainLogItem latestTrainLog,
        long totalEventCount,
        Long eventsSinceLastTrain
    ) {
        static UserModelStatusResponse from(SasrecModelRegistryAdminService.UserModelStatus status) {
            return new UserModelStatusResponse(
                "api",
                "ok",
                Instant.now(),
                status.userId(),
                status.modelStage(),
                status.pmsTrackCount(),
                status.activeModelVersion(),
                status.activeModelGeneratedAt(),
                status.latestTrainLog() == null ? null : TrainLogItem.from(status.latestTrainLog()),
                status.totalEventCount(),
                status.eventsSinceLastTrain()
            );
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record TrainLogItem(
        Long id,
        Instant trainedAt,
        long eventCountAtTrain,
        String modelVersion,
        boolean qualified,
        boolean promoted,
        String summary,
        Double hitRateAtK,
        Double mrrAtK,
        Double ndcgAtK,
        Double baselineHitRateAtK,
        Double baselineMrrAtK,
        Double baselineNdcgAtK,
        Double hitRateDelta,
        Double mrrDelta,
        Double ndcgDelta
    ) {
        static TrainLogItem from(io.myforevermusic.api.modules.recommendation.application.SasrecAutoTrainLogStore.Entry entry) {
            io.myforevermusic.api.modules.recommendation.application.SasrecAutoTrainLogStore.MetricSnapshot metrics =
                entry.metrics() == null
                    ? io.myforevermusic.api.modules.recommendation.application.SasrecAutoTrainLogStore.MetricSnapshot.empty()
                    : entry.metrics();
            return new TrainLogItem(
                entry.id(),
                entry.trainedAt(),
                entry.eventCountAtTrain(),
                entry.modelVersion(),
                entry.qualified(),
                entry.promoted(),
                entry.summary(),
                metrics.hitRateAtK(),
                metrics.mrrAtK(),
                metrics.ndcgAtK(),
                metrics.baselineHitRateAtK(),
                metrics.baselineMrrAtK(),
                metrics.baselineNdcgAtK(),
                metrics.hitRateDelta(),
                metrics.mrrDelta(),
                metrics.ndcgDelta()
            );
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SasrecAutoTrainAdminResponse(
        String service,
        String status,
        Instant generatedAt,
        boolean qualified,
        boolean promoted,
        String modelVersion,
        String summary,
        RecommendationModelTrainingResponse training,
        SasrecRegistryAdminResponse promoteResult
    ) {
        static SasrecAutoTrainAdminResponse from(RecommendationModelTrainingService.AutoTrainResult result) {
            SasrecRegistryAdminResponse promoteResult = result.promoteResult() == null
                ? null
                : SasrecRegistryAdminResponse.from(result.promoteResult());
            return new SasrecAutoTrainAdminResponse(
                "api",
                "ok",
                Instant.now(),
                result.qualified(),
                promoteResult != null,
                result.training().modelVersion(),
                result.summary(),
                result.training(),
                promoteResult
            );
        }
    }
}
