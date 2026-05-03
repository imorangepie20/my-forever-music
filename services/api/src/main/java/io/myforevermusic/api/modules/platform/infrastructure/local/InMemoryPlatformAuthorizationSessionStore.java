package io.myforevermusic.api.modules.platform.infrastructure.local;

import io.myforevermusic.api.common.error.ApiResourceNotFoundException;
import io.myforevermusic.api.modules.platform.application.PlatformAuthorizationSession;
import io.myforevermusic.api.modules.platform.application.PlatformAuthorizationSessionDraft;
import io.myforevermusic.api.modules.platform.application.PlatformAuthorizationSessionStore;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class InMemoryPlatformAuthorizationSessionStore implements PlatformAuthorizationSessionStore {

    private final ConcurrentMap<String, PlatformAuthorizationSession> sessions = new ConcurrentHashMap<>();

    @Override
    public PlatformAuthorizationSession create(PlatformAuthorizationSessionDraft draft) {
        PlatformAuthorizationSession session = new PlatformAuthorizationSession(
            draft.state(),
            draft.userId(),
            draft.platformId(),
            draft.platformDisplayName(),
            draft.authorizationMode(),
            draft.authorizationChannel(),
            List.copyOf(draft.requestedScopes()),
            "pending",
            draft.approvalCode(),
            draft.externalAuthorizationUrl(),
            draft.redirectUri(),
            draft.pkceCodeVerifier(),
            draft.expiresAt(),
            draft.createdAt(),
            null
        );
        sessions.put(draft.state(), session);
        return session;
    }

    @Override
    public Optional<PlatformAuthorizationSession> findByState(String state) {
        return Optional.ofNullable(sessions.get(state));
    }

    @Override
    public PlatformAuthorizationSession markCompleted(String state, Instant completedAt) {
        PlatformAuthorizationSession existing = sessions.get(state);
        if (existing == null) {
            throw new ApiResourceNotFoundException("No pending platform authorization was found for state: %s".formatted(state));
        }

        PlatformAuthorizationSession completed = new PlatformAuthorizationSession(
            existing.state(),
            existing.userId(),
            existing.platformId(),
            existing.platformDisplayName(),
            existing.authorizationMode(),
            existing.authorizationChannel(),
            existing.requestedScopes(),
            "completed",
            existing.approvalCode(),
            existing.externalAuthorizationUrl(),
            existing.redirectUri(),
            existing.pkceCodeVerifier(),
            existing.expiresAt(),
            existing.createdAt(),
            completedAt
        );
        sessions.put(state, completed);
        return completed;
    }
}
