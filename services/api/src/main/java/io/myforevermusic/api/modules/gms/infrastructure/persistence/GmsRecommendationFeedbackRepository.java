package io.myforevermusic.api.modules.gms.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GmsRecommendationFeedbackRepository extends JpaRepository<GmsRecommendationFeedbackEntity, Long> {

    List<GmsRecommendationFeedbackEntity> findByUserIdOrderByCreatedAtDescFeedbackIdDesc(String userId);
}
