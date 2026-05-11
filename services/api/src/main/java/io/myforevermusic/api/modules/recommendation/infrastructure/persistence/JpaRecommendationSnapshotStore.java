package io.myforevermusic.api.modules.recommendation.infrastructure.persistence;

import io.myforevermusic.api.modules.recommendation.application.RecommendationSnapshotStore;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!local")
public class JpaRecommendationSnapshotStore implements RecommendationSnapshotStore {

    private final RecommendationSnapshotRepository repository;

    public JpaRecommendationSnapshotStore(RecommendationSnapshotRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public List<StoredSnapshot> saveAll(List<SnapshotDraft> drafts) {
        return repository.saveAll(drafts.stream()
                .map(RecommendationSnapshotEntity::new)
                .toList())
            .stream()
            .map(RecommendationSnapshotEntity::toState)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredSnapshot> findRecentByUserId(String userId, int limit) {
        return repository.findByUserIdOrderByCreatedAtDescSnapshotIdDesc(userId).stream()
            .limit(Math.max(0, limit))
            .map(RecommendationSnapshotEntity::toState)
            .toList();
    }
}
