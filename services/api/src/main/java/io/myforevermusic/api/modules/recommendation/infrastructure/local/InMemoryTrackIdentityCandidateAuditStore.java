package io.myforevermusic.api.modules.recommendation.infrastructure.local;

import io.myforevermusic.api.modules.recommendation.application.TrackIdentityCandidateAuditStore;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class InMemoryTrackIdentityCandidateAuditStore implements TrackIdentityCandidateAuditStore {

    private final AtomicLong sequence = new AtomicLong(1);
    private final ConcurrentHashMap<Long, Entry> entries = new ConcurrentHashMap<>();

    @Override
    public Entry save(Draft draft) {
        long id = sequence.getAndIncrement();
        Entry entry = new Entry(
            id,
            draft.candidateId(),
            draft.action(),
            draft.emsCollectedTrackId(),
            draft.candidateValue(),
            draft.previousIsrc(),
            draft.newIsrc(),
            draft.status(),
            draft.message(),
            draft.actedBy(),
            draft.actedAt()
        );
        entries.put(id, entry);
        return entry;
    }

    @Override
    public List<Entry> findByCandidateId(Long candidateId) {
        return entries.values().stream()
            .filter(entry -> candidateId.equals(entry.candidateId()))
            .sorted(Comparator.comparing(Entry::actedAt).reversed().thenComparing(Comparator.comparing(Entry::id).reversed()))
            .toList();
    }

    @Override
    public List<Entry> findByCandidateIdAndAction(Long candidateId, String action) {
        return entries.values().stream()
            .filter(entry -> candidateId.equals(entry.candidateId()))
            .filter(entry -> action.equals(entry.action()))
            .sorted(Comparator.comparing(Entry::actedAt).thenComparing(Entry::id))
            .toList();
    }
}
