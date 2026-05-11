package io.myforevermusic.api.modules.recommendation.infrastructure.local;

import io.myforevermusic.api.modules.recommendation.application.RecommendationSnapshotStore;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class InMemoryRecommendationSnapshotStore implements RecommendationSnapshotStore {

    private final AtomicLong sequence = new AtomicLong(1);
    private final Map<Long, StoredSnapshot> snapshotById = new ConcurrentHashMap<>();

    @Override
    public List<StoredSnapshot> saveAll(List<SnapshotDraft> drafts) {
        return drafts.stream()
            .map(this::save)
            .toList();
    }

    @Override
    public List<StoredSnapshot> findRecentByUserId(String userId, int limit) {
        return snapshotById.values().stream()
            .filter(snapshot -> snapshot.userId().equals(userId))
            .sorted(Comparator.comparing(StoredSnapshot::createdAt).reversed())
            .limit(Math.max(0, limit))
            .toList();
    }

    private StoredSnapshot save(SnapshotDraft draft) {
        long snapshotId = sequence.getAndIncrement();
        StoredSnapshot storedSnapshot = new StoredSnapshot(
            snapshotId,
            draft.recommendationId(),
            draft.requestId(),
            draft.userId(),
            draft.candidateTrackId(),
            draft.candidatePlaylistId(),
            draft.candidateTitle(),
            draft.candidateArtistName(),
            draft.sourceSpace(),
            draft.sourcePlatform(),
            draft.modelVersion(),
            draft.featureSnapshotId(),
            draft.affinityScore(),
            draft.noveltyScore(),
            draft.coherenceScore(),
            draft.diversityScore(),
            draft.redundancyPenalty(),
            draft.confidenceScore(),
            draft.rank(),
            draft.reason(),
            draft.createdAt()
        );
        snapshotById.put(snapshotId, storedSnapshot);
        return storedSnapshot;
    }
}
