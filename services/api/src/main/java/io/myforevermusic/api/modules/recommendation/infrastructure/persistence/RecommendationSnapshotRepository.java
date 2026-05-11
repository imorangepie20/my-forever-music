package io.myforevermusic.api.modules.recommendation.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationSnapshotRepository extends JpaRepository<RecommendationSnapshotEntity, Long> {

    List<RecommendationSnapshotEntity> findByUserIdOrderByCreatedAtDescSnapshotIdDesc(String userId);
}
