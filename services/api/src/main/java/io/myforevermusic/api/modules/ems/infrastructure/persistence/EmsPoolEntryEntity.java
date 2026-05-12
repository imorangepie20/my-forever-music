package io.myforevermusic.api.modules.ems.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "ems_pool_entry")
public class EmsPoolEntryEntity {

    public static final String TYPE_PLAYLIST = "playlist";
    public static final String TYPE_TRACK = "track";
    public static final String STATUS_QUEUED = "queued";
    public static final String STATUS_RUNNING = "running";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_FAILED = "failed";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ems_pool_entry_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ems_pool_ingest_run_id", nullable = false)
    private EmsPoolIngestRunEntity run;

    @Column(name = "entry_type", nullable = false, length = 50)
    private String entryType;

    @Column(name = "source_platform", nullable = false, length = 50)
    private String sourcePlatform;

    @Column(name = "external_id", nullable = false, length = 160)
    private String externalId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "artist_name", length = 200)
    private String artistName;

    @Column(name = "curator", length = 120)
    private String curator;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "platform_external_url", length = 500)
    private String platformExternalUrl;

    @Column(name = "platform_uri", length = 200)
    private String platformUri;

    @Column(name = "isrc", length = 32)
    private String isrc;

    @Column(name = "album_title", length = 200)
    private String albumTitle;

    @Column(name = "album_image_url", length = 500)
    private String albumImageUrl;

    @Column(name = "preview_url", length = 500)
    private String previewUrl;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "track_count", nullable = false)
    private int trackCount;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    protected EmsPoolEntryEntity() {}

    public EmsPoolEntryEntity(
        EmsPoolIngestRunEntity run,
        String entryType,
        String sourcePlatform,
        String externalId,
        String title,
        String artistName,
        String curator,
        String description,
        String coverImageUrl,
        String platformExternalUrl,
        String platformUri,
        String isrc,
        String albumTitle,
        String albumImageUrl,
        String previewUrl,
        Integer durationMs,
        int trackCount,
        Instant createdAt
    ) {
        this.run = run;
        this.entryType = entryType;
        this.sourcePlatform = sourcePlatform;
        this.externalId = externalId;
        this.title = title;
        this.artistName = artistName;
        this.curator = curator;
        this.description = description;
        this.coverImageUrl = coverImageUrl;
        this.platformExternalUrl = platformExternalUrl;
        this.platformUri = platformUri;
        this.isrc = isrc;
        this.albumTitle = albumTitle;
        this.albumImageUrl = albumImageUrl;
        this.previewUrl = previewUrl;
        this.durationMs = durationMs;
        this.trackCount = trackCount;
        this.status = STATUS_QUEUED;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void markRunning(Instant now) {
        status = STATUS_RUNNING;
        attempts++;
        updatedAt = now;
    }

    public void markQueued(Instant now) {
        status = STATUS_QUEUED;
        processedAt = null;
        updatedAt = now;
    }

    public void markClaimed(Instant now) {
        status = STATUS_RUNNING;
        updatedAt = now;
    }

    public void markCompleted(Instant now) {
        status = STATUS_COMPLETED;
        lastError = null;
        processedAt = now;
        updatedAt = now;
    }

    public void markFailed(String error, Instant now) {
        status = STATUS_FAILED;
        lastError = error;
        processedAt = now;
        updatedAt = now;
    }

    public Long getId() { return id; }
    public EmsPoolIngestRunEntity getRun() { return run; }
    public String getEntryType() { return entryType; }
    public String getSourcePlatform() { return sourcePlatform; }
    public String getExternalId() { return externalId; }
    public String getTitle() { return title; }
    public String getArtistName() { return artistName; }
    public String getCurator() { return curator; }
    public String getDescription() { return description; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public String getPlatformExternalUrl() { return platformExternalUrl; }
    public String getPlatformUri() { return platformUri; }
    public String getIsrc() { return isrc; }
    public String getAlbumTitle() { return albumTitle; }
    public String getAlbumImageUrl() { return albumImageUrl; }
    public String getPreviewUrl() { return previewUrl; }
    public Integer getDurationMs() { return durationMs; }
    public int getTrackCount() { return trackCount; }
    public String getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public String getLastError() { return lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getProcessedAt() { return processedAt; }
}
