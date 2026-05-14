package io.myforevermusic.api.modules.recommendation.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SasrecAutoTrainLogStore {

    Entry save(Draft draft);

    Optional<Entry> findLatestByUserId(String userId);

    List<Entry> findRecentByUserId(String userId, int limit);

    record Draft(
        String userId,
        Instant trainedAt,
        long eventCountAtTrain,
        String datasetVersion,
        String datasetFingerprint,
        long sequenceItemCountAtTrain,
        long recommendationSnapshotCountAtTrain,
        String modelVersion,
        boolean qualified,
        boolean promoted,
        String summary,
        MetricSnapshot metrics
    ) {}

    record Entry(
        Long id,
        String userId,
        Instant trainedAt,
        long eventCountAtTrain,
        String datasetVersion,
        String datasetFingerprint,
        long sequenceItemCountAtTrain,
        long recommendationSnapshotCountAtTrain,
        String modelVersion,
        boolean qualified,
        boolean promoted,
        String summary,
        MetricSnapshot metrics
    ) {}

    /**
     * 한 학습 tick의 SASRec/baseline/delta metric 묶음. 어느 값이든 null일 수 있다.
     */
    record MetricSnapshot(
        Double hitRateAtK,
        Double mrrAtK,
        Double ndcgAtK,
        Double baselineHitRateAtK,
        Double baselineMrrAtK,
        Double baselineNdcgAtK,
        Double hitRateDelta,
        Double mrrDelta,
        Double ndcgDelta
    ) {
        public static MetricSnapshot empty() {
            return new MetricSnapshot(null, null, null, null, null, null, null, null, null);
        }
    }
}
