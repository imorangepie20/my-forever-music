package io.myforevermusic.api.modules.platform.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PlatformAuthorizationStartResponse(
    String service,
    String status,
    Instant generatedAt,
    AuthorizationUser user,
    AuthorizationSession authorization
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AuthorizationUser(
        String userId,
        String displayName,
        String email
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AuthorizationSession(
        String state,
        String platformId,
        String platformDisplayName,
        String authorizationMode,
        String authorizationChannel,
        List<String> requestedScopes,
        Instant expiresAt,
        String approvalPagePath,
        String callbackPath,
        String sandboxApprovalCode,
        String externalAuthorizationUrl,
        String redirectUri
    ) {
    }
}
