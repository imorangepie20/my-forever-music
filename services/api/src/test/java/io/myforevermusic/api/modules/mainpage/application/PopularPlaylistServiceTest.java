package io.myforevermusic.api.modules.mainpage.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedPlaylistEntity;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedPlaylistRepository;
import io.myforevermusic.api.modules.mainpage.presentation.PopularPlaylistResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class PopularPlaylistServiceTest {

    private final EmsCollectedPlaylistRepository repository = mock(EmsCollectedPlaylistRepository.class);
    private final PopularPlaylistService service = new PopularPlaylistService(repository);

    @Test
    void mapsRepositoryRowsToResponse() {
        when(repository.findPopularByTrackCount(any(Pageable.class)))
            .thenReturn(List.of(
                playlistOf("pl-1", "Top Tracks", "spotify", 120),
                playlistOf("pl-2", "Indie Mix", "tidal", 90)
            ));

        List<PopularPlaylistResponse> result = service.findPopular(6);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).externalPlaylistId()).isEqualTo("pl-1");
        assertThat(result.get(0).trackCount()).isEqualTo(120);
        assertThat(result.get(1).sourcePlatform()).isEqualTo("tidal");
    }

    @Test
    void clampsLimitToAtLeastOne() {
        when(repository.findPopularByTrackCount(any(Pageable.class)))
            .thenReturn(List.of(playlistOf("pl-1", "Hits", "spotify", 50)));

        assertThat(service.findPopular(0)).hasSize(1);
        assertThat(service.findPopular(-3)).hasSize(1);
    }

    @Test
    void returnsEmptyWhenRepositoryEmpty() {
        when(repository.findPopularByTrackCount(any(Pageable.class)))
            .thenReturn(List.of());

        assertThat(service.findPopular(6)).isEmpty();
    }

    private EmsCollectedPlaylistEntity playlistOf(String externalId, String title, String sourcePlatform, int trackCount) {
        return new EmsCollectedPlaylistEntity(
            externalId,
            title,
            sourcePlatform,
            "Curator",
            "Description",
            "https://image/" + externalId,
            "https://platform/" + externalId,
            "spotify:playlist:" + externalId,
            trackCount,
            "search_pool",
            "indie",
            Instant.parse("2026-05-17T09:00:00Z")
        );
    }
}
