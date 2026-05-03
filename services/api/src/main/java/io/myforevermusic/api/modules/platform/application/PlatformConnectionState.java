package io.myforevermusic.api.modules.platform.application;

import java.time.Instant;

public record PlatformConnectionState(
    String userId,
    String platformId,
    boolean connected,
    String connectionStatus,
    String connectionMode,
    String externalAccountLabel,
    String scopeSummary,
    boolean syncReady,
    Instant connectedAt,
    Instant updatedAt
) {
}
