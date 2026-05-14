package io.myforevermusic.api.modules.recommendation.infrastructure.persistence;

import io.myforevermusic.api.modules.recommendation.application.SasrecAutoTrainLogStore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "sasrec_auto_train_log")
public class SasrecAutoTrainLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sasrec_auto_train_log_id")
    private Long id;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "trained_at", nullable = false)
    private Instant trainedAt;

    @Column(name = "event_count_at_train", nullable = false)
    private long eventCountAtTrain;

    @Column(name = "dataset_version", length = 100)
    private String datasetVersion;

    @Column(name = "dataset_fingerprint", length = 100)
    private String datasetFingerprint;

    @Column(name = "sequence_item_count_at_train", nullable = false)
    private long sequenceItemCountAtTrain;

    @Column(name = "recommendation_snapshot_count_at_train", nullable = false)
    private long recommendationSnapshotCountAtTrain;

    @Column(name = "model_version", length = 200)
    private String modelVersion;

    @Column(name = "qualified", nullable = false)
    private boolean qualified;

    @Column(name = "promoted", nullable = false)
    private boolean promoted;

    @Column(name = "summary", length = 1000)
    private String summary;

    @Column(name = "hit_rate_at_k")
    private Double hitRateAtK;

    @Column(name = "mrr_at_k")
    private Double mrrAtK;

    @Column(name = "ndcg_at_k")
    private Double ndcgAtK;

    @Column(name = "baseline_hit_rate_at_k")
    private Double baselineHitRateAtK;

    @Column(name = "baseline_mrr_at_k")
    private Double baselineMrrAtK;

    @Column(name = "baseline_ndcg_at_k")
    private Double baselineNdcgAtK;

    @Column(name = "hit_rate_delta")
    private Double hitRateDelta;

    @Column(name = "mrr_delta")
    private Double mrrDelta;

    @Column(name = "ndcg_delta")
    private Double ndcgDelta;

    protected SasrecAutoTrainLogEntity() {}

    public SasrecAutoTrainLogEntity(SasrecAutoTrainLogStore.Draft draft) {
        this.userId = draft.userId();
        this.trainedAt = draft.trainedAt();
        this.eventCountAtTrain = draft.eventCountAtTrain();
        this.datasetVersion = truncate(draft.datasetVersion(), 100);
        this.datasetFingerprint = truncate(draft.datasetFingerprint(), 100);
        this.sequenceItemCountAtTrain = draft.sequenceItemCountAtTrain();
        this.recommendationSnapshotCountAtTrain = draft.recommendationSnapshotCountAtTrain();
        this.modelVersion = truncate(draft.modelVersion(), 200);
        this.qualified = draft.qualified();
        this.promoted = draft.promoted();
        this.summary = truncate(draft.summary(), 1000);
        SasrecAutoTrainLogStore.MetricSnapshot metrics = draft.metrics();
        if (metrics != null) {
            this.hitRateAtK = metrics.hitRateAtK();
            this.mrrAtK = metrics.mrrAtK();
            this.ndcgAtK = metrics.ndcgAtK();
            this.baselineHitRateAtK = metrics.baselineHitRateAtK();
            this.baselineMrrAtK = metrics.baselineMrrAtK();
            this.baselineNdcgAtK = metrics.baselineNdcgAtK();
            this.hitRateDelta = metrics.hitRateDelta();
            this.mrrDelta = metrics.mrrDelta();
            this.ndcgDelta = metrics.ndcgDelta();
        }
    }

    public SasrecAutoTrainLogStore.Entry toEntry() {
        return new SasrecAutoTrainLogStore.Entry(
            id,
            userId,
            trainedAt,
            eventCountAtTrain,
            datasetVersion,
            datasetFingerprint,
            sequenceItemCountAtTrain,
            recommendationSnapshotCountAtTrain,
            modelVersion,
            qualified,
            promoted,
            summary,
            new SasrecAutoTrainLogStore.MetricSnapshot(
                hitRateAtK,
                mrrAtK,
                ndcgAtK,
                baselineHitRateAtK,
                baselineMrrAtK,
                baselineNdcgAtK,
                hitRateDelta,
                mrrDelta,
                ndcgDelta
            )
        );
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
