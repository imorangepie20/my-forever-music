package io.myforevermusic.api.modules.platform.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformAuthorizationSessionRepository extends JpaRepository<PlatformAuthorizationSessionEntity, Long> {

    Optional<PlatformAuthorizationSessionEntity> findByState(String state);
}
