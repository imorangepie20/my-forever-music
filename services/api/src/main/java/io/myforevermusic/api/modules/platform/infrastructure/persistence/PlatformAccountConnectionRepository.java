package io.myforevermusic.api.modules.platform.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformAccountConnectionRepository extends JpaRepository<PlatformAccountConnectionEntity, Long> {

    List<PlatformAccountConnectionEntity> findByUserId(String userId);

    Optional<PlatformAccountConnectionEntity> findByUserIdAndPlatformId(String userId, String platformId);
}
