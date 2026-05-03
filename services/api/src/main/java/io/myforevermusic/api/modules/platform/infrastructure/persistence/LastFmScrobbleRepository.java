package io.myforevermusic.api.modules.platform.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LastFmScrobbleRepository extends JpaRepository<LastFmScrobbleEntity, Long> {

    Optional<LastFmScrobbleEntity> findByUserIdAndLastFmUsernameAndPlayedAtAndArtistNameAndTrackName(
        String userId,
        String lastFmUsername,
        Instant playedAt,
        String artistName,
        String trackName
    );

    List<LastFmScrobbleEntity> findTop10ByUserIdOrderByPlayedAtDescLastfmScrobbleIdDesc(String userId);

    List<LastFmScrobbleEntity> findTop50ByUserIdOrderByPlayedAtDescLastfmScrobbleIdDesc(String userId);

    long countByUserId(String userId);

    List<LastFmScrobbleEntity> findByUserIdOrderBySyncedAtDescLastfmScrobbleIdDesc(String userId);
}
