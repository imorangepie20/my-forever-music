package io.myforevermusic.api.modules.gms.application;

import java.time.Instant;
import java.util.List;

public interface GmsRecommendationFeedbackStore {

    StoredFeedback save(FeedbackDraft draft);

    List<StoredFeedback> findRecentByUserId(String userId, int limit);

    record FeedbackDraft(
        String userId,
        String requestId,
        String playlistId,
        String trackId,
        String feedbackType,
        Integer score,
        String sourceSpace,
        String reason
    ) {
    }

    record StoredFeedback(
        Long feedbackId,
        String userId,
        String requestId,
        String playlistId,
        String trackId,
        String feedbackType,
        Integer score,
        String sourceSpace,
        String reason,
        Instant createdAt
    ) {
    }
}
