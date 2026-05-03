package io.myforevermusic.api.modules.auth.infrastructure.local;

import io.myforevermusic.api.modules.auth.application.AuthAccountStore;
import io.myforevermusic.api.modules.auth.application.AuthEmailAlreadyRegisteredException;
import io.myforevermusic.api.modules.auth.application.AuthRegisteredAccount;
import io.myforevermusic.api.modules.auth.application.AuthRegistrationDraft;
import io.myforevermusic.api.common.error.ApiResourceNotFoundException;
import java.time.Instant;
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

    @Override
    public Optional<io.myforevermusic.api.modules.auth.application.AuthAuthenticationAccount> findAuthenticationByNormalizedEmail(
        String normalizedEmail
    ) {
        return Optional.ofNullable(accountsByNormalizedEmail.get(normalizedEmail))
            .map(StoredAccount::toAuthenticationAccount);
    }

    @Override
    public AuthRegisteredAccount saveLastFmProfile(String userId, String lastFmUsername, Instant connectedAt) {
        StoredAccount current = accountsByUserId.get(userId);
        if (current == null) {
            throw new ApiResourceNotFoundException("No registered account found for user: %s".formatted(userId));
        }

        StoredAccount updated = current.withLastFmProfile(lastFmUsername, connectedAt);
        accountsByUserId.put(userId, updated);
        accountsByNormalizedEmail.put(updated.normalizedEmail(), updated);
        return updated.toRegisteredAccount();
    }

    @Override
    public AuthRegisteredAccount clearLastFmProfile(String userId) {
        StoredAccount current = accountsByUserId.get(userId);
        if (current == null) {
            throw new ApiResourceNotFoundException("No registered account found for user: %s".formatted(userId));
        }

        StoredAccount updated = current.clearLastFmProfile();
        accountsByUserId.put(userId, updated);
        accountsByNormalizedEmail.put(updated.normalizedEmail(), updated);
        return updated.toRegisteredAccount();
    }

    private record StoredAccount(
        String userId,
        String email,
        String normalizedEmail,
        String displayName,
        String passwordHash,
        String preferredPlatformId,
        String lastFmUsername,
        Instant lastFmConnectedAt,
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
                null,
                null,
                draft.marketingOptIn(),
                draft.onboardingStage(),
                draft.registeredAt(),
                draft.acceptedTermsAt(),
                draft.acceptedPrivacyPolicyAt()
            );
        }

        private StoredAccount withLastFmProfile(String username, Instant connectedAt) {
            return new StoredAccount(
                userId,
                email,
                normalizedEmail,
                displayName,
                passwordHash,
                preferredPlatformId,
                username,
                connectedAt,
                marketingOptIn,
                onboardingStage,
                registeredAt,
                acceptedTermsAt,
                acceptedPrivacyPolicyAt
            );
        }

        private StoredAccount clearLastFmProfile() {
            return withLastFmProfile(null, null);
        }

        private AuthRegisteredAccount toRegisteredAccount() {
            return new AuthRegisteredAccount(
                userId,
                email,
                normalizedEmail,
                displayName,
                preferredPlatformId,
                lastFmUsername,
                lastFmConnectedAt,
                marketingOptIn,
                onboardingStage,
                registeredAt,
                acceptedTermsAt,
                acceptedPrivacyPolicyAt
            );
        }

        private io.myforevermusic.api.modules.auth.application.AuthAuthenticationAccount toAuthenticationAccount() {
            return new io.myforevermusic.api.modules.auth.application.AuthAuthenticationAccount(
                toRegisteredAccount(),
                passwordHash
            );
        }
    }
}
