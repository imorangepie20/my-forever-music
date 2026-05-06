package io.myforevermusic.api.modules.gms.infrastructure.persistence;

import io.myforevermusic.api.modules.gms.application.GmsRecommendationFeedbackStore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "gms_recommendation_feedback")
public class GmsRecommendationFeedbackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Long feedbackId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "request_id", length = 160)
    private String requestId;

    @Column(name = "playlist_id", length = 160)
    private String playlistId;

    @Column(name = "track_id", nullable = false, length = 160)
    private String trackId;

    @Column(name = "feedback_type", nullable = false, length = 30)
    private String feedbackType;

    @Column(name = "feedback_score")
    private Integer score;

    @Column(name = "source_space", length = 50)
    private String sourceSpace;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected GmsRecommendationFeedbackEntity() {
    }

    public GmsRecommendationFeedbackEntity(GmsRecommendationFeedbackStore.FeedbackDraft draft) {
        this.userId = draft.userId();
        this.requestId = draft.requestId();
        this.playlistId = draft.playlistId();
        this.trackId = draft.trackId();
        this.feedbackType = draft.feedbackType();
        this.score = draft.score();
        this.sourceSpace = draft.sourceSpace();
        this.reason = draft.reason();
        this.createdAt = Instant.now();
    }

    public GmsRecommendationFeedbackStore.StoredFeedback toState() {
        return new GmsRecommendationFeedbackStore.StoredFeedback(
            feedbackId,
            userId,
            requestId,
            playlistId,
            trackId,
            feedbackType,
            score,
            sourceSpace,
            reason,
            createdAt
        );
    }
}
