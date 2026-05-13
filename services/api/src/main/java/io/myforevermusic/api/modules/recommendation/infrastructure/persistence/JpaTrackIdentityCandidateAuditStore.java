package io.myforevermusic.api.modules.recommendation.infrastructure.persistence;

import io.myforevermusic.api.modules.recommendation.application.TrackIdentityCandidateAuditStore;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!local")
public class JpaTrackIdentityCandidateAuditStore implements TrackIdentityCandidateAuditStore {

    private final TrackIdentityCandidateAuditRepository repository;

    public JpaTrackIdentityCandidateAuditStore(TrackIdentityCandidateAuditRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Entry save(Draft draft) {
        return repository.save(new TrackIdentityCandidateAuditEntity(draft)).toEntry();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Entry> findByCandidateId(Long candidateId) {
        return repository.findByCandidateIdOrderByActedAtDescIdDesc(candidateId).stream()
            .map(TrackIdentityCandidateAuditEntity::toEntry)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Entry> findByCandidateIdAndAction(Long candidateId, String action) {
        return repository.findByCandidateIdAndActionOrderByActedAtAscIdAsc(candidateId, action).stream()
            .map(TrackIdentityCandidateAuditEntity::toEntry)
            .toList();
    }
}
