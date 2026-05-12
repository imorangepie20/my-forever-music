package io.myforevermusic.api.modules.recommendation.infrastructure.persistence;

import io.myforevermusic.api.modules.recommendation.application.TrackIdentityCandidateStore;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!local")
public class JpaTrackIdentityCandidateStore implements TrackIdentityCandidateStore {

    private final TrackIdentityCandidateRepository repository;

    public JpaTrackIdentityCandidateStore(TrackIdentityCandidateRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Entry save(Draft draft) {
        return repository.save(new TrackIdentityCandidateEntity(draft)).toEntry();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Entry> findById(Long id) {
        return repository.findById(id).map(TrackIdentityCandidateEntity::toEntry);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Entry> findRecentByStatus(String status, int limit) {
        return repository.findRecentByStatus(status, PageRequest.of(0, Math.max(1, limit))).stream()
            .map(TrackIdentityCandidateEntity::toEntry)
            .toList();
    }

    @Override
    @Transactional
    public Entry updateStatus(Long id, String status, String resolvedBy, String notes, Instant resolvedAt) {
        TrackIdentityCandidateEntity entity = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Track identity candidate was not found: " + id));
        entity.resolve(status, resolvedBy, notes, resolvedAt);
        return repository.save(entity).toEntry();
    }
}
