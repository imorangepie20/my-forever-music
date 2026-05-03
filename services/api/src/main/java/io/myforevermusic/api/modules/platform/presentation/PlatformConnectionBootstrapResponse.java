package io.myforevermusic.api.modules.platform.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PlatformConnectionBootstrapResponse(
    String service,
    String status,
    Instant generatedAt,
    ConnectionUser user,
    ConnectionSummary summary,
    List<PlatformConnectionCard> connections
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ConnectionUser(
        String userId,
        String displayName,
        String email,
        String preferredPlatformId,
        String lastFmUsername,
        Instant lastFmConnectedAt
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ConnectionSummary(
        Integer connectedPlatformCount,
        boolean preferredPlatformConnected,
        boolean preferredPlatformReconnectRequired,
        String onboardingStage,
        String nextStepPath,
        String nextStepMessage
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PlatformConnectionCard(
        String platformId,
        String displayName,
        boolean preferred,
        boolean connected,
        String connectionStatus,
        String connectionMode,
        String externalAccountLabel,
        boolean syncReady,
        String credentialStatus,
        boolean reconnectRequired,
        Instant connectedAt,
        String nextActionLabel
    ) {
    }
}
