package io.myforevermusic.api.modules.gms.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.myforevermusic.api.modules.gms.application.GmsRecommendationFeedbackStore;
import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GmsRecommendationFeedbackResponse(
    String service,
    String status,
    Instant processedAt,
    Feedback feedback,
    String nextStepMessage
) {

    public static GmsRecommendationFeedbackResponse from(GmsRecommendationFeedbackStore.StoredFeedback feedback) {
        return new GmsRecommendationFeedbackResponse(
            "gms-recommendation-feedback",
            "recorded",
            feedback.createdAt(),
            new Feedback(
                feedback.feedbackId(),
                feedback.userId(),
                feedback.requestId(),
                feedback.playlistId(),
                feedback.trackId(),
                feedback.feedbackType(),
                feedback.score(),
                feedback.sourceSpace(),
                feedback.reason(),
                feedback.createdAt()
            ),
            "Feedback is now available as PMS learning signal input for future model iterations."
        );
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Feedback(
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
