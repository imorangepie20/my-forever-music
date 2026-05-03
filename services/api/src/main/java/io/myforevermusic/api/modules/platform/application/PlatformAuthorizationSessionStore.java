package io.myforevermusic.api.modules.platform.application;

import java.time.Instant;
import java.util.Optional;

public interface PlatformAuthorizationSessionStore {

    PlatformAuthorizationSession create(PlatformAuthorizationSessionDraft draft);

    Optional<PlatformAuthorizationSession> findByState(String state);

    PlatformAuthorizationSession markCompleted(String state, Instant completedAt);
}
