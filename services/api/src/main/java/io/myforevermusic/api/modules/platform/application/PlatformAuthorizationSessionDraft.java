package io.myforevermusic.api.modules.platform.application;

import java.time.Instant;
import java.util.List;

public record PlatformAuthorizationSessionDraft(
    String state,
    String userId,
    String platformId,
    String platformDisplayName,
    String authorizationMode,
    String authorizationChannel,
    List<String> requestedScopes,
    String approvalCode,
    String externalAuthorizationUrl,
    String redirectUri,
    String pkceCodeVerifier,
    Instant expiresAt,
    Instant createdAt
) {
}
