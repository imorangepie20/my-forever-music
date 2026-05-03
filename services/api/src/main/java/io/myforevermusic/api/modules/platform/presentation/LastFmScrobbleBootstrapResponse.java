package io.myforevermusic.api.modules.platform.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record LastFmScrobbleBootstrapResponse(
    String service,
    String status,
    Instant generatedAt,
    BootstrapUser user,
    BootstrapSummary summary,
    List<ScrobbleItem> recentScrobbles
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record BootstrapUser(
        String userId,
        String lastFmUsername,
        Instant lastFmConnectedAt
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record BootstrapSummary(
        Integer storedScrobbleCount,
        Instant lastSyncedAt,
        Integer returnedScrobbleCount,
        String nextStepMessage
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ScrobbleItem(
        String trackName,
        String artistName,
        String albumName,
        String trackUrl,
        String imageUrl,
        Instant playedAt,
        boolean loved,
        Instant syncedAt
    ) {
    }
}
