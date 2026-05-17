package io.myforevermusic.api.modules.mainpage.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedPlaylistEntity;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedPlaylistRepository;
import io.myforevermusic.api.modules.mainpage.application.PlaylistPopularityRefreshService.RefreshResult;
import io.myforevermusic.api.modules.platform.infrastructure.spotify.SpotifyPublicCatalogClient;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class PlaylistPopularityRefreshServiceTest {

    private final EmsCollectedPlaylistRepository playlistRepository = mock(EmsCollectedPlaylistRepository.class);
    private final SpotifyPublicCatalogClient spotifyPublicCatalogClient = mock(SpotifyPublicCatalogClient.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-17T12:00:00Z"), ZoneOffset.UTC);
    private final PlaylistPopularityRefreshService service = new PlaylistPopularityRefreshService(
        playlistRepository, spotifyPublicCatalogClient, clock
    );

    @Test
    void refreshesNullEntriesAndCountsResults() {
        EmsCollectedPlaylistEntity playlist = playlistOf("pl-1", "spotify", null);
        when(playlistRepository.findStalePopularityCandidates(eq("spotify"), any(Instant.class), any(Pageable.class)))
            .thenReturn(List.of(playlist));
        when(spotifyPublicCatalogClient.getPlaylistFollowers("pl-1")).thenReturn(Optional.of(12_345));

        RefreshResult result = service.refreshSpotify(10);

        assertThat(result.considered()).isEqualTo(1);
        assertThat(result.refreshed()).isEqualTo(1);
        assertThat(result.unchanged()).isZero();
        assertThat(result.skipped()).isZero();
        assertThat(playlist.getFollowersCount()).isEqualTo(12_345);
        assertThat(playlist.getPopularityRefreshedAt()).isEqualTo(Instant.parse("2026-05-17T12:00:00Z"));
    }

    @Test
    void unchangedWhenCountMatchesExisting() {
        EmsCollectedPlaylistEntity playlist = playlistOf("pl-1", "spotify", 50);
        when(playlistRepository.findStalePopularityCandidates(eq("spotify"), any(Instant.class), any(Pageable.class)))
            .thenReturn(List.of(playlist));
        when(spotifyPublicCatalogClient.getPlaylistFollowers("pl-1")).thenReturn(Optional.of(50));

        RefreshResult result = service.refreshSpotify(10);

        assertThat(result.refreshed()).isZero();
        assertThat(result.unchanged()).isEqualTo(1);
    }

    @Test
    void skipsWhenSpotifyReturnsNothing() {
        EmsCollectedPlaylistEntity playlist = playlistOf("pl-1", "spotify", null);
        when(playlistRepository.findStalePopularityCandidates(eq("spotify"), any(Instant.class), any(Pageable.class)))
            .thenReturn(List.of(playlist));
        when(spotifyPublicCatalogClient.getPlaylistFollowers("pl-1")).thenReturn(Optional.empty());

        RefreshResult result = service.refreshSpotify(10);

        assertThat(result.refreshed()).isZero();
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(playlist.getFollowersCount()).isNull();
    }

    private EmsCollectedPlaylistEntity playlistOf(String externalId, String sourcePlatform, Integer existingFollowers) {
        EmsCollectedPlaylistEntity playlist = new EmsCollectedPlaylistEntity(
            externalId,
            "Title " + externalId,
            sourcePlatform,
            "Curator",
            "Description",
            "https://image/" + externalId,
            "https://platform/" + externalId,
            "spotify:playlist:" + externalId,
            100,
            "search_pool",
            "indie",
            Instant.parse("2026-05-10T00:00:00Z")
        );
        if (existingFollowers != null) {
            playlist.applyPopularity(existingFollowers, Instant.parse("2026-05-10T00:00:00Z"));
        }
        return playlist;
    }
}
