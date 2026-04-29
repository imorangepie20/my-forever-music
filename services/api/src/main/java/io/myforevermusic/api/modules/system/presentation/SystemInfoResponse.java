package io.myforevermusic.api.modules.system.presentation;

import java.time.Instant;

public record SystemInfoResponse(
    String service,
    String status,
    String message,
    Instant timestamp
) {
}
