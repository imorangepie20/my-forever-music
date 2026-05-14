package io.myforevermusic.api.modules.recommendation.application;

import io.myforevermusic.api.modules.recommendation.presentation.RecommendationDatasetExportResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RecommendationDatasetExportService {

    private static final String DATASET_VERSION = "recommendation-sequence-v1";
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
        String datasetFingerprint = datasetFingerprint(
            userId.trim(),
            resolvedEventLimit,
            resolvedSnapshotLimit,
            eventItems,
            snapshotItems,
            sequence
        );

        return new RecommendationDatasetExportResponse(
            userId,
            Instant.now(clock),
            resolvedEventLimit,
            resolvedSnapshotLimit,
            new RecommendationDatasetExportResponse.Summary(
                eventItems.size(),
                snapshotItems.size(),
                sequence.size(),
                DATASET_VERSION,
                datasetFingerprint
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

    private String datasetFingerprint(
        String userId,
        int eventLimit,
        int snapshotLimit,
        List<RecommendationDatasetExportResponse.EventItem> events,
        List<RecommendationDatasetExportResponse.RecommendationSnapshotItem> snapshots,
        List<RecommendationDatasetExportResponse.SequenceItem> sequence
    ) {
        StringBuilder input = new StringBuilder();
        appendField(input, DATASET_VERSION);
        appendField(input, userId);
        appendField(input, eventLimit);
        appendField(input, snapshotLimit);
        appendField(input, events.size());
        appendField(input, snapshots.size());
        appendField(input, sequence.size());
        events.forEach(event -> {
            appendField(input, "event");
            appendField(input, event.eventId());
            appendField(input, event.eventType());
            appendField(input, event.eventWeight());
            appendField(input, event.sourceSpace());
            appendField(input, event.sourcePlatform());
            appendField(input, event.trackId());
            appendField(input, event.playlistId());
            appendField(input, event.recommendationId());
            appendField(input, event.metadataConfidence());
            appendField(input, event.occurredAt());
        });
        snapshots.forEach(snapshot -> {
            appendField(input, "snapshot");
            appendField(input, snapshot.snapshotId());
            appendField(input, snapshot.recommendationId());
            appendField(input, snapshot.candidateTrackId());
            appendField(input, snapshot.candidatePlaylistId());
            appendField(input, snapshot.modelVersion());
            appendField(input, snapshot.featureSnapshotId());
            appendField(input, snapshot.affinityScore());
            appendField(input, snapshot.noveltyScore());
            appendField(input, snapshot.coherenceScore());
            appendField(input, snapshot.diversityScore());
            appendField(input, snapshot.redundancyPenalty());
            appendField(input, snapshot.confidenceScore());
            appendField(input, snapshot.rank());
            appendField(input, snapshot.createdAt());
        });
        sequence.forEach(item -> {
            appendField(input, "sequence");
            appendField(input, item.itemType());
            appendField(input, item.sourceId());
            appendField(input, item.token());
            appendField(input, item.trackId());
            appendField(input, item.playlistId());
            appendField(input, item.recommendationId());
            appendField(input, item.weight());
            appendField(input, item.occurredAt());
        });
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return "sha256:" + hex;
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is not available.", ex);
        }
    }

    private void appendField(StringBuilder input, Object value) {
        String text = value == null ? "" : String.valueOf(value);
        input.append(text.length()).append(':').append(text).append('|');
    }
}
