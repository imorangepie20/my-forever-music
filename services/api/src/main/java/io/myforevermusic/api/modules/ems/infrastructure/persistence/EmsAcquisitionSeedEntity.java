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
@Table(name = "ems_acquisition_seed")
public class EmsAcquisitionSeedEntity {

    public static final String STATUS_QUEUED = "queued";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_FAILED = "failed";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ems_acquisition_seed_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ems_acquisition_run_id", nullable = false)
    private EmsAcquisitionRunEntity run;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ems_acquisition_signal_id")
    private EmsAcquisitionSignalEntity signal;

    @Column(name = "platform_id", nullable = false, length = 50)
    private String platformId;

    @Column(name = "query", nullable = false, length = 200)
    private String query;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "ems_pool_ingest_run_id")
    private Long emsPoolIngestRunId;

    @Column(name = "result_playlist_count", nullable = false)
    private int resultPlaylistCount;

    @Column(name = "result_track_count", nullable = false)
    private int resultTrackCount;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected EmsAcquisitionSeedEntity() {}

    public EmsAcquisitionSeedEntity(
        EmsAcquisitionRunEntity run,
        EmsAcquisitionSignalEntity signal,
        String platformId,
        String query,
        Instant createdAt
    ) {
        this.run = run;
        this.signal = signal;
        this.platformId = platformId;
        this.query = query;
        this.status = STATUS_QUEUED;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void markCompleted(Long poolRunId, int playlistCount, int trackCount, Instant now) {
        status = STATUS_COMPLETED;
        emsPoolIngestRunId = poolRunId;
        resultPlaylistCount = playlistCount;
        resultTrackCount = trackCount;
        lastError = null;
        updatedAt = now;
    }

    public void markFailed(String error, Instant now) {
        status = STATUS_FAILED;
        lastError = error;
        updatedAt = now;
    }

    public Long getId() { return id; }
    public EmsAcquisitionRunEntity getRun() { return run; }
    public EmsAcquisitionSignalEntity getSignal() { return signal; }
    public String getPlatformId() { return platformId; }
    public String getQuery() { return query; }
    public String getStatus() { return status; }
    public Long getEmsPoolIngestRunId() { return emsPoolIngestRunId; }
    public int getResultPlaylistCount() { return resultPlaylistCount; }
    public int getResultTrackCount() { return resultTrackCount; }
    public String getLastError() { return lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
