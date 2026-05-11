package io.myforevermusic.api.modules.recommendation.infrastructure.persistence;

import io.myforevermusic.api.modules.recommendation.application.UserMusicEventStore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "user_music_event")
public class UserMusicEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_music_event_id")
    private Long eventId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "event_weight")
    private Double eventWeight;

    @Column(name = "source_space", nullable = false, length = 50)
    private String sourceSpace;

    @Column(name = "source_platform", length = 50)
    private String sourcePlatform;

    @Column(name = "playback_platform_id", length = 50)
    private String playbackPlatformId;

    @Column(name = "item_id", length = 200)
    private String itemId;

    @Column(name = "item_kind", length = 30)
    private String itemKind;

    @Column(name = "track_id", length = 200)
    private String trackId;

    @Column(name = "playlist_id", length = 200)
    private String playlistId;

    @Column(name = "external_track_id", length = 200)
    private String externalTrackId;

    @Column(name = "platform_uri", length = 500)
    private String platformUri;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "artist_name", length = 500)
    private String artistName;

    @Column(name = "album_title", length = 500)
    private String albumTitle;

    @Column(name = "isrc", length = 50)
    private String isrc;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "position_ms")
    private Integer positionMs;

    @Column(name = "play_ratio")
    private Double playRatio;

    @Column(name = "recommendation_id", length = 160)
    private String recommendationId;

    @Column(name = "metadata_confidence")
    private Double metadataConfidence;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected UserMusicEventEntity() {
    }

    public UserMusicEventEntity(UserMusicEventStore.EventDraft draft) {
        this.userId = draft.userId();
        this.eventType = draft.eventType();
        this.eventWeight = draft.eventWeight();
        this.sourceSpace = draft.sourceSpace();
        this.sourcePlatform = draft.sourcePlatform();
        this.playbackPlatformId = draft.playbackPlatformId();
        this.itemId = draft.itemId();
        this.itemKind = draft.itemKind();
        this.trackId = draft.trackId();
        this.playlistId = draft.playlistId();
        this.externalTrackId = draft.externalTrackId();
        this.platformUri = draft.platformUri();
        this.title = draft.title();
        this.artistName = draft.artistName();
        this.albumTitle = draft.albumTitle();
        this.isrc = draft.isrc();
        this.durationMs = draft.durationMs();
        this.positionMs = draft.positionMs();
        this.playRatio = draft.playRatio();
        this.recommendationId = draft.recommendationId();
        this.metadataConfidence = draft.metadataConfidence();
        this.occurredAt = draft.occurredAt();
        this.receivedAt = Instant.now();
    }

    public UserMusicEventStore.StoredEvent toState() {
        return new UserMusicEventStore.StoredEvent(
            eventId,
            userId,
            eventType,
            eventWeight,
            sourceSpace,
            sourcePlatform,
            playbackPlatformId,
            itemId,
            itemKind,
            trackId,
            playlistId,
            externalTrackId,
            platformUri,
            title,
            artistName,
            albumTitle,
            isrc,
            durationMs,
            positionMs,
            playRatio,
            recommendationId,
            metadataConfidence,
            occurredAt,
            receivedAt
        );
    }
}
