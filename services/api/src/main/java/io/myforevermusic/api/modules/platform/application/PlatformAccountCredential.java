package io.myforevermusic.api.modules.platform.application;

import java.time.Duration;
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
        return isExpiringWithin(Duration.ZERO, now);
    }

    public boolean isExpiringWithin(Duration buffer, Instant now) {
        if (accessTokenExpiresAt == null) {
            return false;
        }

        Instant threshold = buffer == null ? now : now.plus(buffer);
        return !accessTokenExpiresAt.isAfter(threshold);
    }
}
