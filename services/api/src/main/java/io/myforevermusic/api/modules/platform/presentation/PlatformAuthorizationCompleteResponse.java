package io.myforevermusic.api.modules.platform.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PlatformAuthorizationCompleteResponse(
    String service,
    String status,
    Instant processedAt,
    AuthorizationResult authorization,
    ConnectionResult connection,
    NextStep nextStep
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AuthorizationResult(
        String state,
        String platformId,
        String platformDisplayName,
        String authorizationMode,
        List<String> requestedScopes,
        Instant completedAt
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ConnectionResult(
        String userId,
        String platformId,
        boolean connected,
        String connectionStatus,
        String connectionMode,
        String externalAccountLabel,
        String scopeSummary,
        boolean syncReady,
        Instant connectedAt
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record NextStep(
        String path,
        String message
    ) {
    }
}
