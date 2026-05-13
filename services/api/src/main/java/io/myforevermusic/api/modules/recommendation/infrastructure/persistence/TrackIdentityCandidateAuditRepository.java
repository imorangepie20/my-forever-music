package io.myforevermusic.api.modules.recommendation.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackIdentityCandidateAuditRepository extends JpaRepository<TrackIdentityCandidateAuditEntity, Long> {

    List<TrackIdentityCandidateAuditEntity> findByCandidateIdOrderByActedAtDescIdDesc(Long candidateId);

    List<TrackIdentityCandidateAuditEntity> findByCandidateIdAndActionOrderByActedAtAscIdAsc(Long candidateId, String action);
}
