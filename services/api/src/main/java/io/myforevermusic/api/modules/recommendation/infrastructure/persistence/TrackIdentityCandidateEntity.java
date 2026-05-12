package io.myforevermusic.api.modules.recommendation.infrastructure.persistence;

import io.myforevermusic.api.modules.recommendation.application.TrackIdentityCandidateStore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "track_identity_candidate")
public class TrackIdentityCandidateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "track_identity_candidate_id")
    private Long id;

    @Column(name = "query_title", nullable = false, length = 500)
    private String queryTitle;

    @Column(name = "query_artist", length = 500)
    private String queryArtist;

    @Column(name = "source", nullable = false, length = 50)
    private String source;

    @Column(name = "candidate_kind", nullable = false, length = 50)
    private String candidateKind;

    @Column(name = "candidate_value", nullable = false, length = 500)
    private String candidateValue;

    @Column(name = "candidate_score")
    private Double candidateScore;

    @Column(name = "metadata", columnDefinition = "text")
    private String metadata;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "notes", length = 1000)
    private String notes;

    protected TrackIdentityCandidateEntity() {}

    public TrackIdentityCandidateEntity(TrackIdentityCandidateStore.Draft draft) {
        this.queryTitle = truncate(draft.queryTitle(), 500);
        this.queryArtist = truncate(draft.queryArtist(), 500);
        this.source = truncate(draft.source(), 50);
        this.candidateKind = truncate(draft.candidateKind(), 50);
        this.candidateValue = truncate(draft.candidateValue(), 500);
        this.candidateScore = draft.candidateScore();
        this.metadata = draft.metadata();
        this.status = TrackIdentityCandidateStore.STATUS_PENDING;
        this.createdBy = truncate(draft.createdBy(), 100);
        this.createdAt = draft.createdAt();
    }

    public void resolve(String status, String resolvedBy, String notes, Instant resolvedAt) {
        this.status = truncate(status, 20);
        this.resolvedBy = truncate(resolvedBy, 100);
        this.resolvedAt = resolvedAt;
        this.notes = truncate(notes, 1000);
    }

    public TrackIdentityCandidateStore.Entry toEntry() {
        return new TrackIdentityCandidateStore.Entry(
            id,
            queryTitle,
            queryArtist,
            source,
            candidateKind,
            candidateValue,
            candidateScore,
            metadata,
            status,
            createdBy,
            createdAt,
            resolvedBy,
            resolvedAt,
            notes
        );
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
