package io.myforevermusic.api.modules.platform.infrastructure.persistence;

import io.myforevermusic.api.common.error.ApiResourceNotFoundException;
import io.myforevermusic.api.modules.platform.application.PlatformAuthorizationSession;
import io.myforevermusic.api.modules.platform.application.PlatformAuthorizationSessionDraft;
import io.myforevermusic.api.modules.platform.application.PlatformAuthorizationSessionStore;
import java.time.Instant;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!local")
public class JpaPlatformAuthorizationSessionStore implements PlatformAuthorizationSessionStore {

    private final PlatformAuthorizationSessionRepository platformAuthorizationSessionRepository;

    public JpaPlatformAuthorizationSessionStore(PlatformAuthorizationSessionRepository platformAuthorizationSessionRepository) {
        this.platformAuthorizationSessionRepository = platformAuthorizationSessionRepository;
    }

    @Override
    public PlatformAuthorizationSession create(PlatformAuthorizationSessionDraft draft) {
        PlatformAuthorizationSessionEntity saved = platformAuthorizationSessionRepository.save(
            new PlatformAuthorizationSessionEntity(
                draft.state(),
                draft.userId(),
                draft.platformId(),
                draft.platformDisplayName(),
                draft.authorizationMode(),
                draft.authorizationChannel(),
                String.join(", ", draft.requestedScopes()),
                "pending",
                draft.approvalCode(),
                draft.externalAuthorizationUrl(),
                draft.redirectUri(),
                draft.pkceCodeVerifier(),
                draft.expiresAt(),
                draft.createdAt(),
                null
            )
        );

        return saved.toSession();
    }

    @Override
    public java.util.Optional<PlatformAuthorizationSession> findByState(String state) {
        return platformAuthorizationSessionRepository.findByState(state).map(PlatformAuthorizationSessionEntity::toSession);
    }

    @Override
    public PlatformAuthorizationSession markCompleted(String state, Instant completedAt) {
        PlatformAuthorizationSessionEntity entity = platformAuthorizationSessionRepository.findByState(state)
            .orElseThrow(() -> new ApiResourceNotFoundException("No pending platform authorization was found for state: %s".formatted(state)));

        entity.markCompleted(completedAt);
        return platformAuthorizationSessionRepository.save(entity).toSession();
    }
}
