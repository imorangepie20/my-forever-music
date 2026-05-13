package io.myforevermusic.api.modules.recommendation.infrastructure.persistence;

import io.myforevermusic.api.modules.recommendation.application.TrackIdentityCandidateAuditStore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "track_identity_candidate_audit")
public class TrackIdentityCandidateAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "track_identity_candidate_audit_id")
    private Long id;

    @Column(name = "track_identity_candidate_id", nullable = false)
    private Long candidateId;

    @Column(name = "action", nullable = false, length = 40)
    private String action;

    @Column(name = "ems_collected_track_id")
    private Long emsCollectedTrackId;

    @Column(name = "candidate_value", length = 500)
    private String candidateValue;

    @Column(name = "previous_isrc", length = 32)
    private String previousIsrc;

    @Column(name = "new_isrc", length = 32)
    private String newIsrc;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "acted_by", length = 100)
    private String actedBy;

    @Column(name = "acted_at", nullable = false)
    private Instant actedAt;

    protected TrackIdentityCandidateAuditEntity() {}

    public TrackIdentityCandidateAuditEntity(TrackIdentityCandidateAuditStore.Draft draft) {
        this.candidateId = draft.candidateId();
        this.action = truncate(draft.action(), 40);
        this.emsCollectedTrackId = draft.emsCollectedTrackId();
        this.candidateValue = truncate(draft.candidateValue(), 500);
        this.previousIsrc = truncate(draft.previousIsrc(), 32);
        this.newIsrc = truncate(draft.newIsrc(), 32);
        this.status = truncate(draft.status(), 40);
        this.message = truncate(draft.message(), 1000);
        this.actedBy = truncate(draft.actedBy(), 100);
        this.actedAt = draft.actedAt();
    }

    public TrackIdentityCandidateAuditStore.Entry toEntry() {
        return new TrackIdentityCandidateAuditStore.Entry(
            id,
            candidateId,
            action,
            emsCollectedTrackId,
            candidateValue,
            previousIsrc,
            newIsrc,
            status,
            message,
            actedBy,
            actedAt
        );
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
