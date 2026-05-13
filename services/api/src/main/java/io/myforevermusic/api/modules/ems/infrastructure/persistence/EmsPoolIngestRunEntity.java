package io.myforevermusic.api.modules.ems.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "ems_pool_ingest_run")
public class EmsPoolIngestRunEntity {

    public static final String STATUS_QUEUED = "queued";
    public static final String STATUS_RUNNING = "running";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_COMPLETED_WITH_ERRORS = "completed_with_errors";
    public static final String STATUS_FAILED = "failed";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ems_pool_ingest_run_id", nullable = false)
    private Long id;

    @Column(name = "requested_by_user_id", nullable = false, length = 100)
    private String requestedByUserId;

    @Column(name = "source_platform", nullable = false, length = 50)
    private String sourcePlatform;

    @Column(name = "search_query", nullable = false, length = 200)
    private String searchQuery;

    @Column(name = "collection_source", nullable = false, length = 50)
    private String collectionSource;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "total_playlist_entries", nullable = false)
    private int totalPlaylistEntries;

    @Column(name = "total_track_entries", nullable = false)
    private int totalTrackEntries;

    @Column(name = "processed_playlist_entries", nullable = false)
    private int processedPlaylistEntries;

    @Column(name = "processed_track_entries", nullable = false)
    private int processedTrackEntries;

    @Column(name = "failed_entries", nullable = false)
    private int failedEntries;

    @Column(name = "collected_playlist_count", nullable = false)
    private int collectedPlaylistCount;

    @Column(name = "collected_track_count", nullable = false)
    private int collectedTrackCount;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected EmsPoolIngestRunEntity() {}

    public EmsPoolIngestRunEntity(
        String requestedByUserId,
        String sourcePlatform,
        String searchQuery,
        int totalPlaylistEntries,
        int totalTrackEntries,
        Instant createdAt
    ) {
        this(
            requestedByUserId,
            sourcePlatform,
            searchQuery,
            "search_pool",
            totalPlaylistEntries,
            totalTrackEntries,
            createdAt
        );
    }

    public EmsPoolIngestRunEntity(
        String requestedByUserId,
        String sourcePlatform,
        String searchQuery,
        String collectionSource,
        int totalPlaylistEntries,
        int totalTrackEntries,
        Instant createdAt
    ) {
        this.requestedByUserId = requestedByUserId;
        this.sourcePlatform = sourcePlatform;
        this.searchQuery = searchQuery;
        this.collectionSource = collectionSource;
        this.status = STATUS_QUEUED;
        this.totalPlaylistEntries = totalPlaylistEntries;
        this.totalTrackEntries = totalTrackEntries;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void markRunning(Instant now) {
        if (startedAt == null) {
            startedAt = now;
        }
        status = STATUS_RUNNING;
        updatedAt = now;
    }

    public void updateProgress(
        int processedPlaylistEntries,
        int processedTrackEntries,
        int failedEntries,
        int collectedPlaylistCount,
        int collectedTrackCount,
        String lastError,
        Instant now
    ) {
        this.processedPlaylistEntries = processedPlaylistEntries;
        this.processedTrackEntries = processedTrackEntries;
        this.failedEntries = failedEntries;
        this.collectedPlaylistCount = collectedPlaylistCount;
        this.collectedTrackCount = collectedTrackCount;
        this.lastError = lastError;
        this.updatedAt = now;
    }

    public void markCompleted(Instant now) {
        status = failedEntries > 0 ? STATUS_COMPLETED_WITH_ERRORS : STATUS_COMPLETED;
        completedAt = now;
        updatedAt = now;
    }

    public void markFailed(String error, Instant now) {
        status = STATUS_FAILED;
        lastError = error;
        completedAt = now;
        updatedAt = now;
    }

    public Long getId() { return id; }
    public String getRequestedByUserId() { return requestedByUserId; }
    public String getSourcePlatform() { return sourcePlatform; }
    public String getSearchQuery() { return searchQuery; }
    public String getCollectionSource() { return collectionSource; }
    public String getStatus() { return status; }
    public int getTotalPlaylistEntries() { return totalPlaylistEntries; }
    public int getTotalTrackEntries() { return totalTrackEntries; }
    public int getProcessedPlaylistEntries() { return processedPlaylistEntries; }
    public int getProcessedTrackEntries() { return processedTrackEntries; }
    public int getFailedEntries() { return failedEntries; }
    public int getCollectedPlaylistCount() { return collectedPlaylistCount; }
    public int getCollectedTrackCount() { return collectedTrackCount; }
    public String getLastError() { return lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
