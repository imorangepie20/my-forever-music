package io.myforevermusic.api.modules.platform.infrastructure.persistence;

import io.myforevermusic.api.modules.platform.application.LastFmScrobbleStore;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!local")
public class JpaLastFmScrobbleStore implements LastFmScrobbleStore {

    private final LastFmScrobbleRepository lastFmScrobbleRepository;

    public JpaLastFmScrobbleStore(LastFmScrobbleRepository lastFmScrobbleRepository) {
        this.lastFmScrobbleRepository = lastFmScrobbleRepository;
    }

    @Override
    @Transactional
    public ScrobbleSaveResult saveScrobbles(
        String userId,
        String lastFmUsername,
        Instant syncedAt,
        List<StoredScrobble> scrobbles
    ) {
        int insertedCount = 0;
        int duplicateCount = 0;

        for (StoredScrobble scrobble : scrobbles) {
            boolean exists = lastFmScrobbleRepository
                .findByUserIdAndLastFmUsernameAndPlayedAtAndArtistNameAndTrackName(
                    userId,
                    lastFmUsername,
                    scrobble.playedAt(),
                    scrobble.artistName(),
                    scrobble.trackName()
                )
                .isPresent();
            if (exists) {
                duplicateCount += 1;
                continue;
            }

            lastFmScrobbleRepository.save(new LastFmScrobbleEntity(scrobble));
            insertedCount += 1;
        }

        return new ScrobbleSaveResult(
            insertedCount,
            duplicateCount,
            getSnapshot(userId, 10)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ScrobbleSnapshot getSnapshot(String userId, int limit) {
        List<LastFmScrobbleEntity> recentEntities = limit <= 10
            ? lastFmScrobbleRepository.findTop10ByUserIdOrderByPlayedAtDescLastfmScrobbleIdDesc(userId)
            : lastFmScrobbleRepository.findTop50ByUserIdOrderByPlayedAtDescLastfmScrobbleIdDesc(userId);
        List<StoredScrobble> recentScrobbles = recentEntities.stream()
            .limit(Math.max(1, limit))
            .map(LastFmScrobbleEntity::toState)
            .toList();

        String username = recentScrobbles.isEmpty() ? null : recentScrobbles.getFirst().lastFmUsername();
        Instant lastSyncedAt = lastFmScrobbleRepository.findByUserIdOrderBySyncedAtDescLastfmScrobbleIdDesc(userId)
            .stream()
            .map(LastFmScrobbleEntity::getSyncedAt)
            .max(Comparator.naturalOrder())
            .orElse(null);

        return new ScrobbleSnapshot(
            userId,
            username,
            (int) lastFmScrobbleRepository.countByUserId(userId),
            lastSyncedAt,
            recentScrobbles
        );
    }
}
