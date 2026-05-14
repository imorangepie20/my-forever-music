package io.myforevermusic.api.modules.recommendation.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationAuditLogRepository extends JpaRepository<RecommendationAuditLogEntity, Long> {

    List<RecommendationAuditLogEntity> findByUserIdOrderByCreatedAtDescAuditLogIdDesc(String userId);
}
