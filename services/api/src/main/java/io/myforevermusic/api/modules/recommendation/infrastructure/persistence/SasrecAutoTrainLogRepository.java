package io.myforevermusic.api.modules.recommendation.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

public interface SasrecAutoTrainLogRepository extends JpaRepository<SasrecAutoTrainLogEntity, Long> {

    Optional<SasrecAutoTrainLogEntity> findFirstByUserIdOrderByTrainedAtDesc(String userId);

    @Query("select entry from SasrecAutoTrainLogEntity entry where entry.userId = :userId order by entry.trainedAt desc")
    List<SasrecAutoTrainLogEntity> findRecentByUserId(@Param("userId") String userId, Pageable pageable);
}
