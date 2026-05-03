package io.myforevermusic.api.modules.auth.infrastructure.local;

import io.myforevermusic.api.modules.auth.application.AuthAccountStore;
import io.myforevermusic.api.modules.auth.application.AuthEmailAlreadyRegisteredException;
import io.myforevermusic.api.modules.auth.application.AuthRegisteredAccount;
import io.myforevermusic.api.modules.auth.application.AuthRegistrationDraft;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class InMemoryAuthAccountStore implements AuthAccountStore {

    private final ConcurrentMap<String, StoredAccount> accountsByNormalizedEmail = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, StoredAccount> accountsByUserId = new ConcurrentHashMap<>();

    @Override
    public AuthRegisteredAccount register(AuthRegistrationDraft draft) {
        StoredAccount storedAccount = new StoredAccount(draft);
        StoredAccount existing = accountsByNormalizedEmail.putIfAbsent(draft.normalizedEmail(), storedAccount);

        if (existing != null) {
            throw new AuthEmailAlreadyRegisteredException(draft.email());
        }

        accountsByUserId.put(draft.userId(), storedAccount);
        return storedAccount.toRegisteredAccount();
    }

    @Override
    public Optional<AuthRegisteredAccount> findByUserId(String userId) {
        return Optional.ofNullable(accountsByUserId.get(userId)).map(StoredAccount::toRegisteredAccount);
    }

    private record StoredAccount(
        String userId,
        String email,
        String normalizedEmail,
        String displayName,
        String passwordHash,
        String preferredPlatformId,
        boolean marketingOptIn,
        String onboardingStage,
        java.time.Instant registeredAt,
        java.time.Instant acceptedTermsAt,
        java.time.Instant acceptedPrivacyPolicyAt
    ) {

        private StoredAccount(AuthRegistrationDraft draft) {
            this(
                draft.userId(),
                draft.email(),
                draft.normalizedEmail(),
                draft.displayName(),
                draft.passwordHash(),
                draft.preferredPlatformId(),
                draft.marketingOptIn(),
                draft.onboardingStage(),
                draft.registeredAt(),
                draft.acceptedTermsAt(),
                draft.acceptedPrivacyPolicyAt()
            );
        }

        private AuthRegisteredAccount toRegisteredAccount() {
            return new AuthRegisteredAccount(
                userId,
                email,
                normalizedEmail,
                displayName,
                preferredPlatformId,
                marketingOptIn,
                onboardingStage,
                registeredAt,
                acceptedTermsAt,
                acceptedPrivacyPolicyAt
            );
        }
    }
}
