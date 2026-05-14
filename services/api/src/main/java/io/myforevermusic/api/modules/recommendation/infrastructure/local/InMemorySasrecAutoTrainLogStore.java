package io.myforevermusic.api.modules.recommendation.infrastructure.local;

import io.myforevermusic.api.modules.recommendation.application.SasrecAutoTrainLogStore;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class InMemorySasrecAutoTrainLogStore implements SasrecAutoTrainLogStore {

    private final AtomicLong sequence = new AtomicLong(1);
    private final ConcurrentHashMap<Long, Entry> entries = new ConcurrentHashMap<>();

    @Override
    public Entry save(Draft draft) {
        long id = sequence.getAndIncrement();
        Entry entry = new Entry(
            id,
            draft.userId(),
            draft.trainedAt(),
            draft.eventCountAtTrain(),
            draft.datasetVersion(),
            draft.datasetFingerprint(),
            draft.sequenceItemCountAtTrain(),
            draft.recommendationSnapshotCountAtTrain(),
            draft.modelVersion(),
            draft.qualified(),
            draft.promoted(),
            draft.summary(),
            draft.metrics() == null ? MetricSnapshot.empty() : draft.metrics()
        );
        entries.put(id, entry);
        return entry;
    }

    @Override
    public Optional<Entry> findLatestByUserId(String userId) {
        return entries.values().stream()
            .filter(entry -> entry.userId().equals(userId))
            .max(Comparator.comparing(Entry::trainedAt));
    }

    @Override
    public List<Entry> findRecentByUserId(String userId, int limit) {
        return entries.values().stream()
            .filter(entry -> entry.userId().equals(userId))
            .sorted(Comparator.comparing(Entry::trainedAt).reversed())
            .limit(Math.max(0, limit))
            .toList();
    }
}
