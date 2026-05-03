package io.myforevermusic.api.modules.auth.infrastructure.persistence;

import io.myforevermusic.api.modules.auth.application.AuthAccountStore;
import io.myforevermusic.api.modules.auth.application.AuthAuthenticationAccount;
import io.myforevermusic.api.modules.auth.application.AuthEmailAlreadyRegisteredException;
import io.myforevermusic.api.modules.auth.application.AuthRegisteredAccount;
import io.myforevermusic.api.modules.auth.application.AuthRegistrationDraft;
import io.myforevermusic.api.common.error.ApiResourceNotFoundException;
import java.time.Instant;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!local")
public class JpaAuthAccountStore implements AuthAccountStore {

    private final AuthUserAccountRepository authUserAccountRepository;

    public JpaAuthAccountStore(AuthUserAccountRepository authUserAccountRepository) {
        this.authUserAccountRepository = authUserAccountRepository;
    }

    @Override
    public AuthRegisteredAccount register(AuthRegistrationDraft draft) {
        if (authUserAccountRepository.existsByNormalizedEmail(draft.normalizedEmail())) {
            throw new AuthEmailAlreadyRegisteredException(draft.email());
        }

        AuthUserAccountEntity saved = authUserAccountRepository.save(
            new AuthUserAccountEntity(
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
            )
        );

        return saved.toRegisteredAccount();
    }

    @Override
    public Optional<AuthRegisteredAccount> findByUserId(String userId) {
        return authUserAccountRepository.findByUserId(userId).map(AuthUserAccountEntity::toRegisteredAccount);
    }

    @Override
    public Optional<AuthAuthenticationAccount> findAuthenticationByNormalizedEmail(String normalizedEmail) {
        return authUserAccountRepository.findByNormalizedEmail(normalizedEmail)
            .map(AuthUserAccountEntity::toAuthenticationAccount);
    }

    @Override
    public AuthRegisteredAccount saveLastFmProfile(String userId, String lastFmUsername, Instant connectedAt) {
        AuthUserAccountEntity entity = authUserAccountRepository.findByUserId(userId)
            .orElseThrow(() -> new ApiResourceNotFoundException("No registered account found for user: %s".formatted(userId)));
        entity.setLastFmProfile(lastFmUsername, connectedAt);
        return authUserAccountRepository.save(entity).toRegisteredAccount();
    }

    @Override
    public AuthRegisteredAccount clearLastFmProfile(String userId) {
        AuthUserAccountEntity entity = authUserAccountRepository.findByUserId(userId)
            .orElseThrow(() -> new ApiResourceNotFoundException("No registered account found for user: %s".formatted(userId)));
        entity.setLastFmProfile(null, null);
        return authUserAccountRepository.save(entity).toRegisteredAccount();
    }
}
