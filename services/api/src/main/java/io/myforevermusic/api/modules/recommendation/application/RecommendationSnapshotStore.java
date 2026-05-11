package io.myforevermusic.api.modules.recommendation.application;

import java.time.Instant;
import java.util.List;

public interface RecommendationSnapshotStore {

    List<StoredSnapshot> saveAll(List<SnapshotDraft> drafts);

    List<StoredSnapshot> findRecentByUserId(String userId, int limit);

    record SnapshotDraft(
        String recommendationId,
        String requestId,
        String userId,
        String candidateTrackId,
        String candidatePlaylistId,
        String candidateTitle,
        String candidateArtistName,
        String sourceSpace,
        String sourcePlatform,
        String modelVersion,
        String featureSnapshotId,
        Double affinityScore,
        Double noveltyScore,
        Double coherenceScore,
        Double diversityScore,
        Double redundancyPenalty,
        Double confidenceScore,
        Integer rank,
        String reason,
        Instant createdAt
    ) {
    }

    record StoredSnapshot(
        Long snapshotId,
        String recommendationId,
        String requestId,
        String userId,
        String candidateTrackId,
        String candidatePlaylistId,
        String candidateTitle,
        String candidateArtistName,
        String sourceSpace,
        String sourcePlatform,
        String modelVersion,
        String featureSnapshotId,
        Double affinityScore,
        Double noveltyScore,
        Double coherenceScore,
        Double diversityScore,
        Double redundancyPenalty,
        Double confidenceScore,
        Integer rank,
        String reason,
        Instant createdAt
    ) {
    }
}
