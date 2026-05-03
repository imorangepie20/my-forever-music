package io.myforevermusic.api.modules.platform.infrastructure.local;

import io.myforevermusic.api.modules.platform.application.LastFmScrobbleStore;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class InMemoryLastFmScrobbleStore implements LastFmScrobbleStore {

    private final ConcurrentMap<String, UserBucket> bucketsByUserId = new ConcurrentHashMap<>();

    @Override
    public ScrobbleSaveResult saveScrobbles(
        String userId,
        String lastFmUsername,
        Instant syncedAt,
        List<StoredScrobble> scrobbles
    ) {
        UserBucket bucket = bucketsByUserId.computeIfAbsent(userId, ignored -> new UserBucket());
        int insertedCount = 0;
        int duplicateCount = 0;

        for (StoredScrobble scrobble : scrobbles) {
            String key = buildKey(scrobble);
            StoredScrobble existing = bucket.scrobblesByKey.putIfAbsent(key, scrobble);
            if (existing == null) {
                insertedCount += 1;
            } else {
                duplicateCount += 1;
            }
        }

        bucket.lastFmUsername = lastFmUsername;
        bucket.lastSyncedAt = syncedAt;

        return new ScrobbleSaveResult(
            insertedCount,
            duplicateCount,
            toSnapshot(userId, bucket, 10)
        );
    }

    @Override
    public ScrobbleSnapshot getSnapshot(String userId, int limit) {
        UserBucket bucket = bucketsByUserId.get(userId);
        if (bucket == null) {
            return new ScrobbleSnapshot(userId, null, 0, null, List.of());
        }

        return toSnapshot(userId, bucket, limit);
    }

    private ScrobbleSnapshot toSnapshot(String userId, UserBucket bucket, int limit) {
        List<StoredScrobble> recentScrobbles = new LinkedHashMap<>(bucket.scrobblesByKey).values().stream()
            .sorted(Comparator.comparing(StoredScrobble::playedAt).reversed()
                .thenComparing(StoredScrobble::artistName, Comparator.nullsLast(String::compareTo))
                .thenComparing(StoredScrobble::trackName, Comparator.nullsLast(String::compareTo)))
            .limit(Math.max(1, limit))
            .toList();

        return new ScrobbleSnapshot(
            userId,
            bucket.lastFmUsername,
            bucket.scrobblesByKey.size(),
            bucket.lastSyncedAt,
            recentScrobbles
        );
    }

    private String buildKey(StoredScrobble scrobble) {
        return "%s|%s|%s|%s".formatted(
            scrobble.lastFmUsername(),
            scrobble.playedAt(),
            scrobble.artistName(),
            scrobble.trackName()
        );
    }

    private static final class UserBucket {
        private final ConcurrentMap<String, StoredScrobble> scrobblesByKey = new ConcurrentHashMap<>();
        private volatile String lastFmUsername;
        private volatile Instant lastSyncedAt;
    }
}
