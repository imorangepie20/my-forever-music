package io.myforevermusic.api.modules.platform.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PlatformConnectionCommandResponse(
    String service,
    String status,
    Instant processedAt,
    ConnectionResult connection,
    NextStep nextStep
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ConnectionResult(
        String userId,
        String platformId,
        String displayName,
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
