package io.myforevermusic.api.modules.recommendation.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPersonalizationProfileRepository extends JpaRepository<UserPersonalizationProfileEntity, Long> {

    Optional<UserPersonalizationProfileEntity> findByUserId(String userId);
}
