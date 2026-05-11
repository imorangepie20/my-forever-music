package io.myforevermusic.api.modules.recommendation.application;

import io.myforevermusic.api.modules.gms.presentation.GmsRecommendationPreviewRequest;
import io.myforevermusic.api.modules.gms.presentation.GmsRecommendationPreviewResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RecommendationSnapshotService {

    private static final String MODEL_VERSION = "gms-baseline-v1";

    private final RecommendationSnapshotStore snapshotStore;
    private final PlaylistQualityEvaluator playlistQualityEvaluator;
    private final Clock clock;

    @Autowired
    public RecommendationSnapshotService(
        RecommendationSnapshotStore snapshotStore,
        PlaylistQualityEvaluator playlistQualityEvaluator
    ) {
        this(snapshotStore, playlistQualityEvaluator, Clock.systemUTC());
    }

    public RecommendationSnapshotService(RecommendationSnapshotStore snapshotStore) {
        this(snapshotStore, new PlaylistQualityEvaluator(), Clock.systemUTC());
    }

    RecommendationSnapshotService(RecommendationSnapshotStore snapshotStore, Clock clock) {
        this(snapshotStore, new PlaylistQualityEvaluator(), clock);
    }

    RecommendationSnapshotService(
        RecommendationSnapshotStore snapshotStore,
        PlaylistQualityEvaluator playlistQualityEvaluator,
        Clock clock
    ) {
        this.snapshotStore = snapshotStore;
        this.playlistQualityEvaluator = playlistQualityEvaluator;
        this.clock = clock;
    }

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
            MODEL_VERSION,
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
