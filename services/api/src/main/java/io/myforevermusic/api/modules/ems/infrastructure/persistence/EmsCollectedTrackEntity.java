package io.myforevermusic.api.modules.ems.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "ems_collected_track")
public class EmsCollectedTrackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ems_collected_track_id", nullable = false)
    private Long id;

    @Column(name = "external_track_id", nullable = false, length = 160)
    private String externalTrackId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "artist_name", nullable = false, length = 200)
    private String artistName;

    @Column(name = "source_platform", nullable = false, length = 50)
    private String sourcePlatform;

    @Column(name = "isrc", length = 32)
    private String isrc;

    @Column(name = "album_title", length = 200)
    private String albumTitle;

    @Column(name = "album_image_url", length = 500)
    private String albumImageUrl;

    @Column(name = "platform_external_url", length = 500)
    private String platformExternalUrl;

    @Column(name = "spotify_uri", length = 200)
    private String spotifyUri;

    @Column(name = "preview_url", length = 500)
    private String previewUrl;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "collection_source", nullable = false, length = 50)
    private String collectionSource;

    @Column(name = "collected_at", nullable = false)
    private Instant collectedAt;

    @Embedded
    private EmsTrackAudioFeatures audioFeatures;

    protected EmsCollectedTrackEntity() {}

    public EmsCollectedTrackEntity(
        String externalTrackId, String title, String artistName,
        String sourcePlatform, String isrc, String albumTitle, String albumImageUrl,
        String platformExternalUrl, String spotifyUri, String previewUrl,
        Integer durationMs, String collectionSource, Instant collectedAt,
        EmsTrackAudioFeatures audioFeatures
    ) {
        this.externalTrackId = externalTrackId;
        this.title = title;
        this.artistName = artistName;
        this.sourcePlatform = sourcePlatform;
        this.isrc = isrc;
        this.albumTitle = albumTitle;
        this.albumImageUrl = albumImageUrl;
        this.platformExternalUrl = platformExternalUrl;
        this.spotifyUri = spotifyUri;
        this.previewUrl = previewUrl;
        this.durationMs = durationMs;
        this.collectionSource = collectionSource;
        this.collectedAt = collectedAt;
        this.audioFeatures = audioFeatures;
    }

    public Long getId() { return id; }
    public String getExternalTrackId() { return externalTrackId; }
    public String getTitle() { return title; }
    public String getArtistName() { return artistName; }
    public String getSourcePlatform() { return sourcePlatform; }
    public String getIsrc() { return isrc; }
    public String getAlbumTitle() { return albumTitle; }
    public String getAlbumImageUrl() { return albumImageUrl; }
    public String getPlatformExternalUrl() { return platformExternalUrl; }
    public String getSpotifyUri() { return spotifyUri; }
    public String getPreviewUrl() { return previewUrl; }
    public Integer getDurationMs() { return durationMs; }
    public String getCollectionSource() { return collectionSource; }
    public Instant getCollectedAt() { return collectedAt; }
    public EmsTrackAudioFeatures getAudioFeatures() { return audioFeatures; }

    public void applyAudioFeatures(EmsTrackAudioFeatures audioFeatures) {
        this.audioFeatures = audioFeatures;
    }
}
