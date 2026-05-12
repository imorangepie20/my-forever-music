package io.myforevermusic.api.modules.recommendation.infrastructure.persistence;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrackIdentityCandidateRepository extends JpaRepository<TrackIdentityCandidateEntity, Long> {

    @Query("select entry from TrackIdentityCandidateEntity entry "
        + "where (:status is null or entry.status = :status) "
        + "order by entry.createdAt desc, entry.id desc")
    List<TrackIdentityCandidateEntity> findRecentByStatus(@Param("status") String status, Pageable pageable);
}
