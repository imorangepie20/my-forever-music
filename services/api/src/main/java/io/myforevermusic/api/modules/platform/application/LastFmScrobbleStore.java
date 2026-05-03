package io.myforevermusic.api.modules.platform.application;

import java.time.Instant;
import java.util.List;

public interface LastFmScrobbleStore {

    ScrobbleSaveResult saveScrobbles(
        String userId,
        String lastFmUsername,
        Instant syncedAt,
        List<StoredScrobble> scrobbles
    );

    ScrobbleSnapshot getSnapshot(String userId, int limit);

    record StoredScrobble(
        String userId,
        String lastFmUsername,
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

    record ScrobbleSaveResult(
        int insertedCount,
        int duplicateCount,
        ScrobbleSnapshot snapshot
    ) {
    }

    record ScrobbleSnapshot(
        String userId,
        String lastFmUsername,
        int storedCount,
        Instant lastSyncedAt,
        List<StoredScrobble> recentScrobbles
    ) {
    }
}
