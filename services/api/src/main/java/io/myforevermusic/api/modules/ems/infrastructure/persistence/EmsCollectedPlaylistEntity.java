package io.myforevermusic.api.modules.ems.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "ems_collected_playlist")
public class EmsCollectedPlaylistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ems_collected_playlist_id", nullable = false)
    private Long id;

    @Column(name = "external_playlist_id", nullable = false, length = 160)
    private String externalPlaylistId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "source_platform", nullable = false, length = 50)
    private String sourcePlatform;

    @Column(name = "curator", nullable = false, length = 120)
    private String curator;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "platform_external_url", length = 500)
    private String platformExternalUrl;

    @Column(name = "spotify_uri", length = 200)
    private String spotifyUri;

    @Column(name = "track_count", nullable = false)
    private int trackCount;

    @Column(name = "collection_source", nullable = false, length = 50)
    private String collectionSource;

    @Column(name = "search_query", length = 200)
    private String searchQuery;

    @Column(name = "collected_at", nullable = false)
    private Instant collectedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    protected EmsCollectedPlaylistEntity() {}

    public EmsCollectedPlaylistEntity(
        String externalPlaylistId, String title, String sourcePlatform,
        String curator, String description, String coverImageUrl,
        String platformExternalUrl, String spotifyUri, int trackCount,
        String collectionSource, String searchQuery, Instant collectedAt
    ) {
        this.externalPlaylistId = externalPlaylistId;
        this.title = title;
        this.sourcePlatform = sourcePlatform;
        this.curator = curator;
        this.description = description;
        this.coverImageUrl = coverImageUrl;
        this.platformExternalUrl = platformExternalUrl;
        this.spotifyUri = spotifyUri;
        this.trackCount = trackCount;
        this.collectionSource = collectionSource;
        this.searchQuery = searchQuery;
        this.collectedAt = collectedAt;
    }

    public void applyCollectedMetadata(
        String title,
        String curator,
        String description,
        String coverImageUrl,
        String platformExternalUrl,
        String spotifyUri,
        int trackCount,
        String collectionSource,
        String searchQuery,
        Instant collectedAt
    ) {
        this.title = title;
        this.curator = curator;
        this.description = description;
        this.coverImageUrl = coverImageUrl;
        this.platformExternalUrl = platformExternalUrl;
        this.spotifyUri = spotifyUri;
        this.trackCount = trackCount;
        this.collectionSource = collectionSource;
        this.searchQuery = searchQuery;
        this.collectedAt = collectedAt;
    }

    public Long getId() { return id; }
    public String getExternalPlaylistId() { return externalPlaylistId; }
    public String getTitle() { return title; }
    public String getSourcePlatform() { return sourcePlatform; }
    public String getCurator() { return curator; }
    public String getDescription() { return description; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public String getPlatformExternalUrl() { return platformExternalUrl; }
    public String getSpotifyUri() { return spotifyUri; }
    public int getTrackCount() { return trackCount; }
    public String getCollectionSource() { return collectionSource; }
    public String getSearchQuery() { return searchQuery; }
    public Instant getCollectedAt() { return collectedAt; }
    public Instant getExpiresAt() { return expiresAt; }
}
