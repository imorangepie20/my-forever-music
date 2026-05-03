package io.myforevermusic.api.modules.platform.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformAccountCredentialRepository extends JpaRepository<PlatformAccountCredentialEntity, Long> {

    Optional<PlatformAccountCredentialEntity> findByUserIdAndPlatformId(String userId, String platformId);

    void deleteByUserIdAndPlatformId(String userId, String platformId);
}
