package io.myforevermusic.api.modules.recommendation.application;

import io.myforevermusic.api.modules.auth.application.AuthAccountStore;
import io.myforevermusic.api.modules.pms.application.PmsUserLibraryStore;
import io.myforevermusic.api.modules.recommendation.infrastructure.ai.AiSasrecRegistryClient;
import io.myforevermusic.api.modules.recommendation.infrastructure.ai.AiSasrecRegistryClient.SasrecRegistryResponse;
import io.myforevermusic.api.modules.recommendation.infrastructure.ai.AiSasrecTrainingClient;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SasrecModelRegistryAdminService {

    private static final String ADMIN_EMAIL = "jowoosungtidal@gmail.com";

    private final AiSasrecRegistryClient registryClient;
    private final AuthAccountStore authAccountStore;
    private final PmsUserLibraryStore pmsUserLibraryStore;
    private final UserMusicEventStore eventStore;
    private final SasrecAutoTrainLogStore trainLogStore;
    private final RecommendationModelTrainingService trainingService;

    @Autowired
    public SasrecModelRegistryAdminService(
        AiSasrecRegistryClient registryClient,
        AuthAccountStore authAccountStore,
        PmsUserLibraryStore pmsUserLibraryStore,
        UserMusicEventStore eventStore,
        SasrecAutoTrainLogStore trainLogStore,
        @Lazy RecommendationModelTrainingService trainingService
    ) {
        this.registryClient = registryClient;
        this.authAccountStore = authAccountStore;
        this.pmsUserLibraryStore = pmsUserLibraryStore;
        this.eventStore = eventStore;
        this.trainLogStore = trainLogStore;
        this.trainingService = trainingService;
    }

    public SasrecRegistryResponse latest(String adminUserId) {
        assertAdmin(adminUserId);
        return registryClient.latest(adminUserId);
    }

    public SasrecRegistryResponse promote(String adminUserId, String modelVersion) {
        assertAdmin(adminUserId);
        if (modelVersion == null || modelVersion.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "model_version is required.");
        }
        return registryClient.promote(adminUserId, modelVersion);
    }

    public SasrecRegistryResponse disable(String adminUserId, String modelVersion) {
        assertAdmin(adminUserId);
        if (modelVersion == null || modelVersion.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "model_version is required.");
        }
        return registryClient.disable(adminUserId, modelVersion);
    }

    public SasrecRegistryResponse rollback(String adminUserId) {
        assertAdmin(adminUserId);
        return registryClient.rollback(adminUserId);
    }

    public RecommendationModelTrainingService.AutoTrainResult autoTrainAndPromote(
        String adminUserId,
        Integer eventLimit,
        Integer snapshotLimit,
        AiSasrecTrainingClient.SasrecTrainingOptions trainingOptions
    ) {
        assertAdmin(adminUserId);
        return trainingService.autoTrainAndPromote(adminUserId, eventLimit, snapshotLimit, trainingOptions);
    }

    public UserModelStatus getUserModelStatus(String adminUserId, String targetUserId) {
        assertAdmin(adminUserId);
        if (targetUserId == null || targetUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "target user_id is required.");
        }

        long pmsTrackCount = pmsUserLibraryStore.findPlaylists(targetUserId).stream()
            .mapToLong(playlist -> playlist.trackCount())
            .sum();

        SasrecRegistryResponse activeModel;
        try {
            activeModel = registryClient.latest(targetUserId);
        } catch (Exception ex) {
            activeModel = null;
        }
        boolean hasActiveModel = activeModel != null
            && "ok".equals(activeModel.status())
            && activeModel.modelVersion() != null
            && !activeModel.modelVersion().isBlank();

        Optional<SasrecAutoTrainLogStore.Entry> latestTrainLog = trainLogStore.findLatestByUserId(targetUserId);

        long totalEventCount = 0L;
        try {
            totalEventCount = eventStore.countEventsByUserIdAfter(targetUserId, Instant.EPOCH);
        } catch (Exception ignored) {
        }

        Long eventsSinceLastTrain = null;
        if (latestTrainLog.isPresent() && latestTrainLog.get().trainedAt() != null) {
            try {
                eventsSinceLastTrain = eventStore.countEventsByUserIdAfter(
                    targetUserId,
                    latestTrainLog.get().trainedAt()
                );
            } catch (Exception ignored) {
            }
        }

        String stage;
        if (pmsTrackCount == 0L) {
            stage = "cold-start";
        } else if (hasActiveModel) {
            stage = "personalized";
        } else {
            stage = "baseline";
        }

        return new UserModelStatus(
            targetUserId,
            stage,
            pmsTrackCount,
            hasActiveModel ? activeModel.modelVersion() : null,
            hasActiveModel ? activeModel.generatedAt() : null,
            latestTrainLog.orElse(null),
            totalEventCount,
            eventsSinceLastTrain
        );
    }

    public record UserModelStatus(
        String userId,
        String modelStage,
        long pmsTrackCount,
        String activeModelVersion,
        String activeModelGeneratedAt,
        SasrecAutoTrainLogStore.Entry latestTrainLog,
        long totalEventCount,
        Long eventsSinceLastTrain
    ) {}

    private void assertAdmin(String userId) {
        String normalizedEmail = authAccountStore.findByUserId(userId)
            .map(account -> account.normalizedEmail())
            .orElse("");
        if (!ADMIN_EMAIL.equals(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "SASRec model registry admin access is restricted.");
        }
    }
}
