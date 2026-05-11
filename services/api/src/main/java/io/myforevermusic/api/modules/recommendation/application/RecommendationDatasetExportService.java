package io.myforevermusic.api.modules.recommendation.application;

import io.myforevermusic.api.modules.recommendation.presentation.RecommendationDatasetExportResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RecommendationDatasetExportService {

    private static final int DEFAULT_EVENT_LIMIT = 300;
    private static final int DEFAULT_SNAPSHOT_LIMIT = 200;
    private static final int MAX_LIMIT = 1_000;

    private final UserMusicEventStore eventStore;
    private final RecommendationSnapshotStore snapshotStore;
    private final Clock clock;

    @Autowired
    public RecommendationDatasetExportService(
        UserMusicEventStore eventStore,
        RecommendationSnapshotStore snapshotStore
    ) {
        this(eventStore, snapshotStore, Clock.systemUTC());
    }

    RecommendationDatasetExportService(
        UserMusicEventStore eventStore,
        RecommendationSnapshotStore snapshotStore,
        Clock clock
    ) {
        this.eventStore = eventStore;
        this.snapshotStore = snapshotStore;
        this.clock = clock;
    }

    public RecommendationDatasetExportResponse exportUserSequence(
        String userId,
        Integer eventLimit,
        Integer snapshotLimit
    ) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("user_id is required to export recommendation dataset.");
        }

        int resolvedEventLimit = resolveLimit(eventLimit, DEFAULT_EVENT_LIMIT);
        int resolvedSnapshotLimit = resolveLimit(snapshotLimit, DEFAULT_SNAPSHOT_LIMIT);
        List<UserMusicEventStore.StoredEvent> events = eventStore.findRecentByUserId(userId, resolvedEventLimit)
            .stream()
            .sorted(Comparator.comparing(UserMusicEventStore.StoredEvent::occurredAt)
                .thenComparing(UserMusicEventStore.StoredEvent::eventId))
            .toList();
        List<RecommendationSnapshotStore.StoredSnapshot> snapshots = snapshotStore
            .findRecentByUserId(userId, resolvedSnapshotLimit)
            .stream()
            .sorted(Comparator.comparing(RecommendationSnapshotStore.StoredSnapshot::createdAt)
                .thenComparing(RecommendationSnapshotStore.StoredSnapshot::rank, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(RecommendationSnapshotStore.StoredSnapshot::snapshotId))
            .toList();

        List<RecommendationDatasetExportResponse.EventItem> eventItems = events.stream()
            .map(RecommendationDatasetExportResponse.EventItem::from)
            .toList();
        List<RecommendationDatasetExportResponse.RecommendationSnapshotItem> snapshotItems = snapshots.stream()
            .map(RecommendationDatasetExportResponse.RecommendationSnapshotItem::from)
            .toList();
        List<RecommendationDatasetExportResponse.SequenceItem> sequence = Stream.concat(
                events.stream().map(RecommendationDatasetExportResponse.SequenceItem::fromEvent),
                snapshots.stream().map(RecommendationDatasetExportResponse.SequenceItem::fromSnapshot)
            )
            .sorted(Comparator.comparing(RecommendationDatasetExportResponse.SequenceItem::occurredAt)
                .thenComparing(RecommendationDatasetExportResponse.SequenceItem::itemType)
                .thenComparing(RecommendationDatasetExportResponse.SequenceItem::sourceId))
            .toList();

        return new RecommendationDatasetExportResponse(
            userId,
            Instant.now(clock),
            resolvedEventLimit,
            resolvedSnapshotLimit,
            new RecommendationDatasetExportResponse.Summary(
                eventItems.size(),
                snapshotItems.size(),
                sequence.size()
            ),
            eventItems,
            snapshotItems,
            sequence
        );
    }

    private int resolveLimit(Integer limit, int defaultLimit) {
        if (limit == null) {
            return defaultLimit;
        }
        return Math.min(MAX_LIMIT, Math.max(1, limit));
    }
}
