package io.myforevermusic.api.modules.platform.application;

import java.time.Instant;

public record PlatformAccountCredential(
    String userId,
    String platformId,
    String authorizationMode,
    String externalUserId,
    String externalAccountLabel,
    String accessToken,
    String refreshToken,
    String tokenType,
    String scopeSummary,
    Instant accessTokenExpiresAt,
    Instant issuedAt,
    Instant updatedAt
) {

    public boolean isExpired(Instant now) {
        return accessTokenExpiresAt != null && accessTokenExpiresAt.isBefore(now);
    }
}
