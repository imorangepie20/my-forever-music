package io.myforevermusic.api.modules.user.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "user_track_like")
public class UserTrackLikeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_track_like_id", nullable = false)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "source_platform", nullable = false, length = 50)
    private String sourcePlatform;

    @Column(name = "external_track_id", nullable = false, length = 200)
    private String externalTrackId;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "artist_name", length = 500)
    private String artistName;

    @Column(name = "album_title", length = 500)
    private String albumTitle;

    @Column(name = "image_url", length = 800)
    private String imageUrl;

    @Column(name = "spotify_track_id", length = 100)
    private String spotifyTrackId;

    @Column(name = "platform_external_url", length = 800)
    private String platformExternalUrl;

    @Column(name = "liked_at", nullable = false)
    private Instant likedAt;

    protected UserTrackLikeEntity() {}

    public UserTrackLikeEntity(
        String userId,
        String sourcePlatform,
        String externalTrackId,
        String title,
        String artistName,
        String albumTitle,
        String imageUrl,
        String spotifyTrackId,
        String platformExternalUrl,
        Instant likedAt
    ) {
        this.userId = userId;
        this.sourcePlatform = sourcePlatform;
        this.externalTrackId = externalTrackId;
        this.title = title;
        this.artistName = artistName;
        this.albumTitle = albumTitle;
        this.imageUrl = imageUrl;
        this.spotifyTrackId = spotifyTrackId;
        this.platformExternalUrl = platformExternalUrl;
        this.likedAt = likedAt;
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public String getSourcePlatform() { return sourcePlatform; }
    public String getExternalTrackId() { return externalTrackId; }
    public String getTitle() { return title; }
    public String getArtistName() { return artistName; }
    public String getAlbumTitle() { return albumTitle; }
    public String getImageUrl() { return imageUrl; }
    public String getSpotifyTrackId() { return spotifyTrackId; }
    public String getPlatformExternalUrl() { return platformExternalUrl; }
    public Instant getLikedAt() { return likedAt; }
}
