package io.myforevermusic.api.modules.recommendation.application;

import io.myforevermusic.api.modules.auth.application.AuthAccountStore;
import io.myforevermusic.api.modules.gms.presentation.GmsRecommendationPreviewRequest;
import io.myforevermusic.api.modules.gms.presentation.GmsRecommendationPreviewResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RecommendationSnapshotService {

    private static final String MODEL_VERSION = "gms-baseline-v1";
    private static final String ADMIN_EMAIL = "jowoosungtidal@gmail.com";

    private final RecommendationSnapshotStore snapshotStore;
    private final PlaylistQualityEvaluator playlistQualityEvaluator;
    private final AuthAccountStore authAccountStore;
    private final Clock clock;

    @Autowired
    public RecommendationSnapshotService(
        RecommendationSnapshotStore snapshotStore,
        PlaylistQualityEvaluator playlistQualityEvaluator,
        AuthAccountStore authAccountStore
    ) {
        this(snapshotStore, playlistQualityEvaluator, authAccountStore, Clock.systemUTC());
    }

    public RecommendationSnapshotService(RecommendationSnapshotStore snapshotStore) {
        this(snapshotStore, new PlaylistQualityEvaluator(), null, Clock.systemUTC());
    }

    RecommendationSnapshotService(RecommendationSnapshotStore snapshotStore, Clock clock) {
        this(snapshotStore, new PlaylistQualityEvaluator(), null, clock);
    }

    RecommendationSnapshotService(
        RecommendationSnapshotStore snapshotStore,
        PlaylistQualityEvaluator playlistQualityEvaluator,
        Clock clock
    ) {
        this(snapshotStore, playlistQualityEvaluator, null, clock);
    }

    RecommendationSnapshotService(
        RecommendationSnapshotStore snapshotStore,
        PlaylistQualityEvaluator playlistQualityEvaluator,
        AuthAccountStore authAccountStore,
        Clock clock
    ) {
        this.snapshotStore = snapshotStore;
        this.playlistQualityEvaluator = playlistQualityEvaluator;
        this.authAccountStore = authAccountStore;
        this.clock = clock;
    }

    public List<PlaylistQualityAdminSummary> summarizeRecentForAdmin(String adminUserId, int limit) {
        assertAdmin(adminUserId);
        int safeLimit = Math.min(50, Math.max(1, limit));
        int snapshotFetchSize = Math.min(500, safeLimit * 20);
        List<RecommendationSnapshotStore.StoredSnapshot> snapshots =
            snapshotStore.findRecentByUserId(adminUserId, snapshotFetchSize);

        Map<String, List<RecommendationSnapshotStore.StoredSnapshot>> groups = new LinkedHashMap<>();
        for (RecommendationSnapshotStore.StoredSnapshot snapshot : snapshots) {
            String key = snapshot.recommendationId() == null ? "" : snapshot.recommendationId();
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(snapshot);
        }

        List<PlaylistQualityAdminSummary> summaries = new ArrayList<>(groups.size());
        for (Map.Entry<String, List<RecommendationSnapshotStore.StoredSnapshot>> entry : groups.entrySet()) {
            List<RecommendationSnapshotStore.StoredSnapshot> items = entry.getValue();
            if (items.isEmpty()) {
                continue;
            }
            RecommendationSnapshotStore.StoredSnapshot head = items.get(0);
            summaries.add(new PlaylistQualityAdminSummary(
                head.recommendationId(),
                head.userId(),
                head.createdAt(),
                head.modelVersion(),
                items.size(),
                average(items, RecommendationSnapshotStore.StoredSnapshot::affinityScore),
                average(items, RecommendationSnapshotStore.StoredSnapshot::noveltyScore),
                head.coherenceScore(),
                head.diversityScore(),
                head.redundancyPenalty(),
                average(items, RecommendationSnapshotStore.StoredSnapshot::confidenceScore)
            ));
        }

        summaries.sort((left, right) -> {
            Instant leftCreatedAt = left.createdAt() == null ? Instant.EPOCH : left.createdAt();
            Instant rightCreatedAt = right.createdAt() == null ? Instant.EPOCH : right.createdAt();
            return rightCreatedAt.compareTo(leftCreatedAt);
        });

        if (summaries.size() > safeLimit) {
            return summaries.subList(0, safeLimit);
        }
        return summaries;
    }

    private void assertAdmin(String userId) {
        if (authAccountStore == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin user store is not configured.");
        }
        String normalizedEmail = authAccountStore.findByUserId(userId)
            .map(account -> account.normalizedEmail())
            .orElse("");
        if (!ADMIN_EMAIL.equals(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Playlist quality admin access is restricted.");
        }
    }

    private Double average(
        List<RecommendationSnapshotStore.StoredSnapshot> items,
        java.util.function.Function<RecommendationSnapshotStore.StoredSnapshot, Double> extractor
    ) {
        double sum = 0.0d;
        int count = 0;
        for (RecommendationSnapshotStore.StoredSnapshot item : items) {
            Double value = extractor.apply(item);
            if (value == null || value.isNaN() || value.isInfinite()) {
                continue;
            }
            sum += value;
            count++;
        }
        if (count == 0) {
            return null;
        }
        double avg = sum / count;
        return Math.round(avg * 100.0d) / 100.0d;
    }

    public record PlaylistQualityAdminSummary(
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
    ) {}

    public List<RecommendationSnapshotStore.StoredSnapshot> recordGmsPreview(
        GmsRecommendationPreviewRequest request,
        GmsRecommendationPreviewResponse response
    ) {
        if (request.userId() == null || request.userId().isBlank() || response.items() == null || response.items().isEmpty()) {
            return List.of();
        }

        Instant createdAt = Instant.now(clock);
        PlaylistQualityEvaluation playlistEvaluation = playlistQualityEvaluator.evaluate(response.items());
        List<RecommendationSnapshotStore.SnapshotDraft> drafts = response.items().stream()
            .map(item -> toDraft(request, response, item, playlistEvaluation, createdAt))
            .toList();
        return snapshotStore.saveAll(drafts);
    }

    private RecommendationSnapshotStore.SnapshotDraft toDraft(
        GmsRecommendationPreviewRequest request,
        GmsRecommendationPreviewResponse response,
        GmsRecommendationPreviewResponse.RecommendationItem item,
        PlaylistQualityEvaluation playlistEvaluation,
        Instant createdAt
    ) {
        return new RecommendationSnapshotStore.SnapshotDraft(
            response.requestId(),
            request.requestId(),
            request.userId(),
            item.trackId(),
            item.sourcePlaylistId(),
            item.title(),
            item.artistName(),
            item.sourceSpace(),
            item.sourcePlatform(),
            resolveModelVersion(response),
            item.audioFeatureTrackId(),
            clamp(item.score()),
            noveltyScore(request.familiarityBias()),
            playlistEvaluation.coherenceScore(),
            playlistEvaluation.diversityScore(),
            playlistEvaluation.redundancyPenalty(),
            confidenceScore(item),
            item.rank(),
            item.reason(),
            createdAt
        );
    }

    private Double noveltyScore(Integer familiarityBias) {
        if (familiarityBias == null) {
            return null;
        }
        return clamp(1.0d - ((Math.min(5, Math.max(1, familiarityBias)) - 1.0d) / 4.0d));
    }

    private String resolveModelVersion(GmsRecommendationPreviewResponse response) {
        if (
            response.context() != null
                && response.context().engine() != null
                && !response.context().engine().isBlank()
                && response.context().engine().contains("sasrec:")
        ) {
            return response.context().engine();
        }
        return MODEL_VERSION;
    }

    private Double confidenceScore(GmsRecommendationPreviewResponse.RecommendationItem item) {
        double score = 0.55d;
        if (item.trackId() != null && !item.trackId().isBlank()) {
            score += 0.15d;
        }
        if (item.audioFeatureTrackId() != null && !item.audioFeatureTrackId().isBlank()) {
            score += 0.15d;
        }
        if (item.sourcePlaylistId() != null && !item.sourcePlaylistId().isBlank()) {
            score += 0.10d;
        }
        return clamp(score);
    }

    private Double clamp(Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            return null;
        }
        return Math.min(1.0d, Math.max(0.0d, value));
    }
}
