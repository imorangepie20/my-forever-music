package io.myforevermusic.api.modules.recommendation.infrastructure.persistence;

import io.myforevermusic.api.modules.recommendation.application.RecommendationSnapshotStore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "recommendation_snapshot")
public class RecommendationSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_snapshot_id")
    private Long snapshotId;

    @Column(name = "recommendation_id", nullable = false, length = 160)
    private String recommendationId;

    @Column(name = "request_id", length = 160)
    private String requestId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "candidate_track_id", length = 200)
    private String candidateTrackId;

    @Column(name = "candidate_playlist_id", length = 200)
    private String candidatePlaylistId;

    @Column(name = "candidate_title", length = 500)
    private String candidateTitle;

    @Column(name = "candidate_artist_name", length = 500)
    private String candidateArtistName;

    @Column(name = "source_space", length = 50)
    private String sourceSpace;

    @Column(name = "source_platform", length = 50)
    private String sourcePlatform;

    @Column(name = "model_version", nullable = false, length = 80)
    private String modelVersion;

    @Column(name = "feature_snapshot_id", length = 160)
    private String featureSnapshotId;

    @Column(name = "affinity_score")
    private Double affinityScore;

    @Column(name = "novelty_score")
    private Double noveltyScore;

    @Column(name = "coherence_score")
    private Double coherenceScore;

    @Column(name = "diversity_score")
    private Double diversityScore;

    @Column(name = "redundancy_penalty")
    private Double redundancyPenalty;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "rank")
    private Integer rank;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RecommendationSnapshotEntity() {
    }

    public RecommendationSnapshotEntity(RecommendationSnapshotStore.SnapshotDraft draft) {
        this.recommendationId = draft.recommendationId();
        this.requestId = draft.requestId();
        this.userId = draft.userId();
        this.candidateTrackId = draft.candidateTrackId();
        this.candidatePlaylistId = draft.candidatePlaylistId();
        this.candidateTitle = draft.candidateTitle();
        this.candidateArtistName = draft.candidateArtistName();
        this.sourceSpace = draft.sourceSpace();
        this.sourcePlatform = draft.sourcePlatform();
        this.modelVersion = draft.modelVersion();
        this.featureSnapshotId = draft.featureSnapshotId();
        this.affinityScore = draft.affinityScore();
        this.noveltyScore = draft.noveltyScore();
        this.coherenceScore = draft.coherenceScore();
        this.diversityScore = draft.diversityScore();
        this.redundancyPenalty = draft.redundancyPenalty();
        this.confidenceScore = draft.confidenceScore();
        this.rank = draft.rank();
        this.reason = draft.reason();
        this.createdAt = draft.createdAt();
    }

    public RecommendationSnapshotStore.StoredSnapshot toState() {
        return new RecommendationSnapshotStore.StoredSnapshot(
            snapshotId,
            recommendationId,
            requestId,
            userId,
            candidateTrackId,
            candidatePlaylistId,
            candidateTitle,
            candidateArtistName,
            sourceSpace,
            sourcePlatform,
            modelVersion,
            featureSnapshotId,
            affinityScore,
            noveltyScore,
            coherenceScore,
            diversityScore,
            redundancyPenalty,
            confidenceScore,
            rank,
            reason,
            createdAt
        );
    }
}
