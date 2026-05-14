package io.myforevermusic.api.modules.recommendation.infrastructure.persistence;

import io.myforevermusic.api.modules.recommendation.application.RecommendationAuditLogStore;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!local")
public class JpaRecommendationAuditLogStore implements RecommendationAuditLogStore {

    private final RecommendationAuditLogRepository repository;

    public JpaRecommendationAuditLogStore(RecommendationAuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public StoredAuditLog save(AuditDraft draft) {
        return repository.save(new RecommendationAuditLogEntity(draft)).toState();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredAuditLog> findRecentByUserId(String userId, int limit) {
        return repository.findByUserIdOrderByCreatedAtDescAuditLogIdDesc(userId).stream()
            .limit(Math.max(0, limit))
            .map(RecommendationAuditLogEntity::toState)
            .toList();
    }
}
