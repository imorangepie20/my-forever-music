package io.myforevermusic.api.modules.recommendation.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "user_personalization_profile")
public class UserPersonalizationProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_personalization_profile_id")
    private Long id;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "top_artists_json", columnDefinition = "text")
    private String topArtistsJson;

    @Column(name = "top_source_platforms_json", columnDefinition = "text")
    private String topSourcePlatformsJson;

    @Column(name = "event_count_at_update", nullable = false)
    private long eventCountAtUpdate;

    @Column(name = "last_event_at")
    private Instant lastEventAt;

    @Column(name = "recomputed_at", nullable = false)
    private Instant recomputedAt;

    protected UserPersonalizationProfileEntity() {}

    public UserPersonalizationProfileEntity(
        String userId,
        String topArtistsJson,
        String topSourcePlatformsJson,
        long eventCountAtUpdate,
        Instant lastEventAt,
        Instant recomputedAt
    ) {
        this.userId = userId;
        this.topArtistsJson = topArtistsJson;
        this.topSourcePlatformsJson = topSourcePlatformsJson;
        this.eventCountAtUpdate = eventCountAtUpdate;
        this.lastEventAt = lastEventAt;
        this.recomputedAt = recomputedAt;
    }

    public Long getId() { return id; }

    public String getUserId() { return userId; }

    public String getTopArtistsJson() { return topArtistsJson; }

    public String getTopSourcePlatformsJson() { return topSourcePlatformsJson; }

    public long getEventCountAtUpdate() { return eventCountAtUpdate; }

    public Instant getLastEventAt() { return lastEventAt; }

    public Instant getRecomputedAt() { return recomputedAt; }

    public void apply(
        String topArtistsJson,
        String topSourcePlatformsJson,
        long eventCountAtUpdate,
        Instant lastEventAt,
        Instant recomputedAt
    ) {
        this.topArtistsJson = topArtistsJson;
        this.topSourcePlatformsJson = topSourcePlatformsJson;
        this.eventCountAtUpdate = eventCountAtUpdate;
        this.lastEventAt = lastEventAt;
        this.recomputedAt = recomputedAt;
    }
}
