package io.myforevermusic.api.modules.recommendation.infrastructure.persistence;

import io.myforevermusic.api.modules.recommendation.application.CanonicalTrackIdentityStore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "canonical_track_identity")
public class CanonicalTrackIdentityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "canonical_track_identity_id")
    private Long id;

    @Column(name = "canonical_track_id", nullable = false)
    private Long canonicalTrackId;

    @Column(name = "identity_kind", nullable = false, length = 50)
    private String identityKind;

    @Column(name = "identity_value", nullable = false, length = 500)
    private String identityValue;

    @Column(name = "source", nullable = false, length = 50)
    private String source;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "created_from_candidate_id")
    private Long createdFromCandidateId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CanonicalTrackIdentityEntity() {}

    public CanonicalTrackIdentityEntity(
        Long canonicalTrackId,
        String identityKind,
        String identityValue,
        String source,
        Double confidenceScore,
        Long createdFromCandidateId,
        Instant now
    ) {
        this.canonicalTrackId = canonicalTrackId;
        this.identityKind = truncate(identityKind, 50);
        this.identityValue = truncate(identityValue, 500);
        this.source = truncate(source, 50);
        this.confidenceScore = confidenceScore;
        this.status = CanonicalTrackIdentityStore.STATUS_ACTIVE;
        this.createdFromCandidateId = createdFromCandidateId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public CanonicalTrackIdentityStore.IdentityEntry toEntry() {
        return new CanonicalTrackIdentityStore.IdentityEntry(
            id,
            canonicalTrackId,
            identityKind,
            identityValue,
            source,
            confidenceScore,
            status,
            createdFromCandidateId,
            createdAt,
            updatedAt
        );
    }

    public Long getCanonicalTrackId() {
        return canonicalTrackId;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
