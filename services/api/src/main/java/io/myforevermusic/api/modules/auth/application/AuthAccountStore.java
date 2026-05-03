package io.myforevermusic.api.modules.auth.application;

import java.util.Optional;
import java.time.Instant;

public interface AuthAccountStore {

    AuthRegisteredAccount register(AuthRegistrationDraft draft);

    Optional<AuthRegisteredAccount> findByUserId(String userId);

    Optional<AuthAuthenticationAccount> findAuthenticationByNormalizedEmail(String normalizedEmail);

    AuthRegisteredAccount saveLastFmProfile(String userId, String lastFmUsername, Instant connectedAt);

    AuthRegisteredAccount clearLastFmProfile(String userId);
}
