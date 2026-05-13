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
@Table(name = "canonical_track")
public class CanonicalTrackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "canonical_track_id")
    private Long id;

    @Column(name = "display_title", nullable = false, length = 500)
    private String displayTitle;

    @Column(name = "display_artist_name", length = 500)
    private String displayArtistName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CanonicalTrackEntity() {}

    public CanonicalTrackEntity(String displayTitle, String displayArtistName, Instant now) {
        this.displayTitle = truncate(displayTitle, 500);
        this.displayArtistName = truncate(displayArtistName, 500);
        this.createdAt = now;
        this.updatedAt = now;
    }

    public CanonicalTrackIdentityStore.CanonicalTrackEntry toEntry() {
        return new CanonicalTrackIdentityStore.CanonicalTrackEntry(
            id,
            displayTitle,
            displayArtistName,
            createdAt,
            updatedAt
        );
    }

    public Long getId() {
        return id;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
