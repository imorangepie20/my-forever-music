package io.myforevermusic.api.modules.recommendation.infrastructure.local;

import io.myforevermusic.api.modules.recommendation.application.TrackIdentityCandidateStore;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class InMemoryTrackIdentityCandidateStore implements TrackIdentityCandidateStore {

    private final AtomicLong sequence = new AtomicLong(1);
    private final ConcurrentHashMap<Long, Entry> entries = new ConcurrentHashMap<>();

    @Override
    public Entry save(Draft draft) {
        long id = sequence.getAndIncrement();
        Entry entry = new Entry(
            id,
            draft.queryTitle(),
            draft.queryArtist(),
            draft.source(),
            draft.candidateKind(),
            draft.candidateValue(),
            draft.candidateScore(),
            draft.metadata(),
            STATUS_PENDING,
            draft.createdBy(),
            draft.createdAt(),
            null,
            null,
            null
        );
        entries.put(id, entry);
        return entry;
    }

    @Override
    public Optional<Entry> findById(Long id) {
        return Optional.ofNullable(entries.get(id));
    }

    @Override
    public List<Entry> findRecentByStatus(String status, int limit) {
        return entries.values().stream()
            .filter(entry -> status == null || status.equals(entry.status()))
            .sorted(Comparator.comparing(Entry::createdAt).reversed())
            .limit(Math.max(0, limit))
            .toList();
    }

    @Override
    public Entry updateStatus(Long id, String status, String resolvedBy, String notes, Instant resolvedAt) {
        Entry existing = entries.get(id);
        if (existing == null) {
            throw new IllegalArgumentException("Track identity candidate was not found: " + id);
        }
        Entry updated = new Entry(
            existing.id(),
            existing.queryTitle(),
            existing.queryArtist(),
            existing.source(),
            existing.candidateKind(),
            existing.candidateValue(),
            existing.candidateScore(),
            existing.metadata(),
            status,
            existing.createdBy(),
            existing.createdAt(),
            resolvedBy,
            resolvedAt,
            notes
        );
        entries.put(id, updated);
        return updated;
    }
}
