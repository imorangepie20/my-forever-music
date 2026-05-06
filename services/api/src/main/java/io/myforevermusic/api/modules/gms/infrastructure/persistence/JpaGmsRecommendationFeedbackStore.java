package io.myforevermusic.api.modules.gms.infrastructure.persistence;

import io.myforevermusic.api.modules.gms.application.GmsRecommendationFeedbackStore;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!local")
public class JpaGmsRecommendationFeedbackStore implements GmsRecommendationFeedbackStore {

    private final GmsRecommendationFeedbackRepository repository;

    public JpaGmsRecommendationFeedbackStore(GmsRecommendationFeedbackRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public StoredFeedback save(FeedbackDraft draft) {
        return repository.save(new GmsRecommendationFeedbackEntity(draft)).toState();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredFeedback> findRecentByUserId(String userId, int limit) {
        return repository.findByUserIdOrderByCreatedAtDescFeedbackIdDesc(userId).stream()
            .limit(Math.max(0, limit))
            .map(GmsRecommendationFeedbackEntity::toState)
            .toList();
    }
}
