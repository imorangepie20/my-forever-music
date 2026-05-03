package io.myforevermusic.api.modules.auth.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthUserAccountRepository extends JpaRepository<AuthUserAccountEntity, String> {

    boolean existsByNormalizedEmail(String normalizedEmail);

    Optional<AuthUserAccountEntity> findByUserId(String userId);
}
