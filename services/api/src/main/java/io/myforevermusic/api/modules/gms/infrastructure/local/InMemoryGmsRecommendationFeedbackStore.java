package io.myforevermusic.api.modules.gms.infrastructure.local;

import io.myforevermusic.api.modules.gms.application.GmsRecommendationFeedbackStore;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class InMemoryGmsRecommendationFeedbackStore implements GmsRecommendationFeedbackStore {

    private final AtomicLong sequence = new AtomicLong(1);
    private final Map<Long, StoredFeedback> feedbackById = new ConcurrentHashMap<>();

    @Override
    public StoredFeedback save(FeedbackDraft draft) {
        long feedbackId = sequence.getAndIncrement();
        StoredFeedback storedFeedback = new StoredFeedback(
            feedbackId,
            draft.userId(),
            draft.requestId(),
            draft.playlistId(),
            draft.trackId(),
            draft.feedbackType(),
            draft.score(),
            draft.sourceSpace(),
            draft.reason(),
            Instant.now()
        );
        feedbackById.put(feedbackId, storedFeedback);
        return storedFeedback;
    }

    @Override
    public List<StoredFeedback> findRecentByUserId(String userId, int limit) {
        return feedbackById.values().stream()
            .filter(feedback -> feedback.userId().equals(userId))
            .sorted(Comparator.comparing(StoredFeedback::createdAt).reversed())
            .limit(Math.max(0, limit))
            .toList();
    }
}
