package io.myforevermusic.api.modules.platform.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record LastFmScrobbleSyncResponse(
    String service,
    String status,
    Instant processedAt,
    SyncResult sync,
    List<LastFmScrobbleBootstrapResponse.ScrobbleItem> recentScrobbles,
    List<String> notes
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SyncResult(
        String userId,
        String lastFmUsername,
        Integer fetchedTrackCount,
        Integer insertedScrobbleCount,
        Integer duplicateScrobbleCount,
        Integer skippedNowPlayingCount,
        Integer storedScrobbleCount,
        Instant lastSyncedAt
    ) {
    }
}
