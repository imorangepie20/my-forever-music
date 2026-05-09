package io.myforevermusic.api.modules.platform.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PlatformPlaybackCredentialsResponse(
    String service,
    String status,
    Instant generatedAt,
    String userId,
    String platformId,
    String accessToken,
    String tokenType,
    String scopeSummary,
    List<String> scopes,
    Instant expiresAt,
    String externalUserId,
    String externalAccountLabel,
    String authorizationMode,
    String clientId,
    String countryCode
) {
}
