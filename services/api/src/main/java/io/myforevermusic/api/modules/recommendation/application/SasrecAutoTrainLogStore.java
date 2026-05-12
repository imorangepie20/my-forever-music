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
        String modelVersion,
        boolean qualified,
        boolean promoted,
        String summary
    ) {}

    record Entry(
        Long id,
        String userId,
        Instant trainedAt,
        long eventCountAtTrain,
        String modelVersion,
        boolean qualified,
        boolean promoted,
        String summary
    ) {}
}
