package io.myforevermusic.api.modules.platform.application;

import java.time.Instant;
import java.util.List;

public record PlatformAuthorizationSession(
    String state,
    String userId,
    String platformId,
    String platformDisplayName,
    String authorizationMode,
    String authorizationChannel,
    List<String> requestedScopes,
    String status,
    String approvalCode,
    String externalAuthorizationUrl,
    String redirectUri,
    String pkceCodeVerifier,
    Instant expiresAt,
    Instant createdAt,
    Instant completedAt
) {

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }

    public boolean isCompleted() {
        return completedAt != null || "completed".equals(status);
    }
}
