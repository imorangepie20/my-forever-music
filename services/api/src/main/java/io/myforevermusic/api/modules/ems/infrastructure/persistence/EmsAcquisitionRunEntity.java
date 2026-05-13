package io.myforevermusic.api.modules.ems.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "ems_acquisition_run")
public class EmsAcquisitionRunEntity {

    public static final String STATUS_RUNNING = "running";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_COMPLETED_WITH_FAILURES = "completed_with_failures";
    public static final String STATUS_FAILED = "failed";
    public static final String STATUS_SKIPPED = "skipped";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ems_acquisition_run_id", nullable = false)
    private Long id;

    @Column(name = "trigger_type", nullable = false, length = 50)
    private String triggerType;

    @Column(name = "requested_by_user_id", nullable = false, length = 100)
    private String requestedByUserId;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "source_count", nullable = false)
    private int sourceCount;

    @Column(name = "article_count", nullable = false)
    private int articleCount;

    @Column(name = "skipped_article_count", nullable = false)
    private int skippedArticleCount;

    @Column(name = "signal_count", nullable = false)
    private int signalCount;

    @Column(name = "seed_count", nullable = false)
    private int seedCount;

    @Column(name = "skipped_seed_count", nullable = false)
    private int skippedSeedCount;

    @Column(name = "pool_run_count", nullable = false)
    private int poolRunCount;

    @Column(name = "failed_source_count", nullable = false)
    private int failedSourceCount;

    @Column(name = "failed_seed_count", nullable = false)
    private int failedSeedCount;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected EmsAcquisitionRunEntity() {}

    public EmsAcquisitionRunEntity(String triggerType, String requestedByUserId, Instant startedAt) {
        this.triggerType = triggerType;
        this.requestedByUserId = requestedByUserId;
        this.status = STATUS_RUNNING;
        this.startedAt = startedAt;
        this.updatedAt = startedAt;
    }

    public void markSkipped(String message, Instant now) {
        status = STATUS_SKIPPED;
        this.message = message;
        completedAt = now;
        updatedAt = now;
    }

    public void updateProgress(
        int sourceCount,
        int articleCount,
        int skippedArticleCount,
        int signalCount,
        int seedCount,
        int skippedSeedCount,
        int poolRunCount,
        int failedSourceCount,
        int failedSeedCount,
        Instant now
    ) {
        this.sourceCount = sourceCount;
        this.articleCount = articleCount;
        this.skippedArticleCount = skippedArticleCount;
        this.signalCount = signalCount;
        this.seedCount = seedCount;
        this.skippedSeedCount = skippedSeedCount;
        this.poolRunCount = poolRunCount;
        this.failedSourceCount = failedSourceCount;
        this.failedSeedCount = failedSeedCount;
        this.updatedAt = now;
    }

    public void markCompleted(String message, Instant now) {
        status = failedSourceCount > 0 || failedSeedCount > 0
            ? STATUS_COMPLETED_WITH_FAILURES
            : STATUS_COMPLETED;
        this.message = message;
        completedAt = now;
        updatedAt = now;
    }

    public void markFailed(String error, Instant now) {
        status = STATUS_FAILED;
        lastError = error;
        message = error;
        completedAt = now;
        updatedAt = now;
    }

    public Long getId() { return id; }
    public String getTriggerType() { return triggerType; }
    public String getRequestedByUserId() { return requestedByUserId; }
    public String getStatus() { return status; }
    public int getSourceCount() { return sourceCount; }
    public int getArticleCount() { return articleCount; }
    public int getSkippedArticleCount() { return skippedArticleCount; }
    public int getSignalCount() { return signalCount; }
    public int getSeedCount() { return seedCount; }
    public int getSkippedSeedCount() { return skippedSeedCount; }
    public int getPoolRunCount() { return poolRunCount; }
    public int getFailedSourceCount() { return failedSourceCount; }
    public int getFailedSeedCount() { return failedSeedCount; }
    public String getMessage() { return message; }
    public String getLastError() { return lastError; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
