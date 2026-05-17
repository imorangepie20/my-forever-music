package io.myforevermusic.api.modules.melon.application;

import io.myforevermusic.api.modules.melon.infrastructure.persistence.MelonChartTrackEntity;
import io.myforevermusic.api.modules.melon.infrastructure.persistence.MelonChartTrackRepository;
import io.myforevermusic.api.modules.melon.presentation.MelonResolveResponse;
import io.myforevermusic.api.modules.platform.infrastructure.spotify.SpotifyPublicCatalogClient;
import io.myforevermusic.api.modules.platform.infrastructure.spotify.SpotifyPublicCatalogClient.PublicTrack;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Resolves a Melon chart row into a playable Spotify track via the public
 * catalogue search. Falls back to title-only search when artist-prefixed query
 * misses, which happens occasionally with romanised K-pop releases.
 */
@Service
public class MelonChartResolverService {

    private static final Logger log = LoggerFactory.getLogger(MelonChartResolverService.class);
    private static final int SEARCH_LIMIT = 5;

    private final MelonChartTrackRepository repository;
    private final SpotifyPublicCatalogClient spotifyPublicCatalogClient;

    public MelonChartResolverService(
        MelonChartTrackRepository repository,
        SpotifyPublicCatalogClient spotifyPublicCatalogClient
    ) {
        this.repository = repository;
        this.spotifyPublicCatalogClient = spotifyPublicCatalogClient;
    }

    public Optional<MelonResolveResponse> resolveByRank(int rank) {
        return repository.findAll().stream()
            .filter(track -> track.getRank() == rank)
            .findFirst()
            .map(this::resolve);
    }

    private MelonResolveResponse resolve(MelonChartTrackEntity entity) {
        String primaryQuery = "track:\"%s\" artist:\"%s\"".formatted(
            sanitize(entity.getTitle()),
            sanitize(entity.getArtistName())
        );
        List<PublicTrack> hits = spotifyPublicCatalogClient.searchTracks(primaryQuery, SEARCH_LIMIT);
        if (hits.isEmpty()) {
            String fallbackQuery = "%s %s".formatted(sanitize(entity.getArtistName()), sanitize(entity.getTitle()));
            hits = spotifyPublicCatalogClient.searchTracks(fallbackQuery, SEARCH_LIMIT);
        }

        if (hits.isEmpty()) {
            log.debug("Spotify resolve missed for Melon rank={} title='{}'", entity.getRank(), entity.getTitle());
            return new MelonResolveResponse(
                entity.getRank(),
                entity.getMelonSongId(),
                entity.getTitle(),
                entity.getArtistName(),
                null,
                null,
                null,
                null,
                null,
                null,
                false
            );
        }

        PublicTrack best = hits.get(0);
        return new MelonResolveResponse(
            entity.getRank(),
            entity.getMelonSongId(),
            entity.getTitle(),
            entity.getArtistName(),
            best.spotifyTrackId(),
            best.title(),
            best.artistName(),
            best.albumTitle(),
            best.imageUrl() != null ? best.imageUrl() : entity.getImageUrl(),
            best.externalUrl(),
            true
        );
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "").trim();
    }
}
