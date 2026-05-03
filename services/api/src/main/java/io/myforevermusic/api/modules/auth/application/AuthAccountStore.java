package io.myforevermusic.api.modules.auth.application;

import java.util.Optional;

public interface AuthAccountStore {

    AuthRegisteredAccount register(AuthRegistrationDraft draft);

    Optional<AuthRegisteredAccount> findByUserId(String userId);
}
