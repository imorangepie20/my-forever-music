package io.myforevermusic.api.modules.platform.application;

import java.time.Instant;

public record PlatformConnectionDraft(
    String userId,
    String platformId,
    String connectionMode,
    String externalAccountLabel,
    String scopeSummary,
    boolean syncReady,
    Instant connectedAt,
    Instant updatedAt
) {
}
