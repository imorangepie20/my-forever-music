package io.myforevermusic.api.modules.auth.infrastructure.persistence;

import io.myforevermusic.api.modules.auth.application.AuthAccountStore;
import io.myforevermusic.api.modules.auth.application.AuthEmailAlreadyRegisteredException;
import io.myforevermusic.api.modules.auth.application.AuthRegisteredAccount;
import io.myforevermusic.api.modules.auth.application.AuthRegistrationDraft;
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
}
