package io.myforevermusic.api.modules.platform.infrastructure.persistence;

import io.myforevermusic.api.modules.platform.application.LastFmScrobbleStore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "lastfm_scrobble")
public class LastFmScrobbleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lastfm_scrobble_id", nullable = false)
    private Long lastfmScrobbleId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "last_fm_username", nullable = false, length = 120)
    private String lastFmUsername;

    @Column(name = "track_name", nullable = false, length = 300)
    private String trackName;

    @Column(name = "artist_name", nullable = false, length = 300)
    private String artistName;

    @Column(name = "album_name", length = 300)
    private String albumName;

    @Column(name = "track_url", length = 500)
    private String trackUrl;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "played_at", nullable = false)
    private Instant playedAt;

    @Column(name = "loved", nullable = false)
    private boolean loved;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    protected LastFmScrobbleEntity() {
    }

    public LastFmScrobbleEntity(LastFmScrobbleStore.StoredScrobble scrobble) {
        this.userId = scrobble.userId();
        this.lastFmUsername = scrobble.lastFmUsername();
        this.trackName = scrobble.trackName();
        this.artistName = scrobble.artistName();
        this.albumName = scrobble.albumName();
        this.trackUrl = scrobble.trackUrl();
        this.imageUrl = scrobble.imageUrl();
        this.playedAt = scrobble.playedAt();
        this.loved = scrobble.loved();
        this.syncedAt = scrobble.syncedAt();
    }

    public LastFmScrobbleStore.StoredScrobble toState() {
        return new LastFmScrobbleStore.StoredScrobble(
            userId,
            lastFmUsername,
            trackName,
            artistName,
            albumName,
            trackUrl,
            imageUrl,
            playedAt,
            loved,
            syncedAt
        );
    }

    public Instant getSyncedAt() {
        return syncedAt;
    }
}
