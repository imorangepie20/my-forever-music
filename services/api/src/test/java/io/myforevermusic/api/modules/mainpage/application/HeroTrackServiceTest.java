package io.myforevermusic.api.modules.mainpage.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedTrackEntity;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedTrackRepository;
import io.myforevermusic.api.modules.mainpage.presentation.HeroTrackResponse;
import io.myforevermusic.api.modules.recommendation.application.RecommendationSnapshotStore;
import io.myforevermusic.api.modules.recommendation.application.RecommendationSnapshotStore.StoredSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class HeroTrackServiceTest {

    private final RecommendationSnapshotStore snapshotStore = mock(RecommendationSnapshotStore.class);
    private final EmsCollectedTrackRepository trackRepository = mock(EmsCollectedTrackRepository.class);
    private final HeroTrackService service = new HeroTrackService(snapshotStore, trackRepository);

    @Test
    void returnsTopGmsRecommendationWhenUserHasSnapshot() {
        StoredSnapshot top = snapshotOf("rec-1", 1, "spotify-track-123", "spotify", "Spotify Smash", "Top Artist");
        StoredSnapshot lower = snapshotOf("rec-2", 4, "spotify-track-456", "spotify", "Other", "Other Artist");
        when(snapshotStore.findRecentByUserId(eq("user-1"), anyInt())).thenReturn(List.of(lower, top));
        when(trackRepository.findBySourcePlatformAndExternalTrackId("spotify", "spotify-track-123"))
            .thenReturn(Optional.of(trackOf(
                "spotify-track-123",
                "spotify",
                "spotify:track:spotify-track-123",
                "https://preview/123",
                "Spotify Smash",
                "Top Artist"
            )));

        Optional<HeroTrackResponse> response = service.resolve("user-1");

        assertThat(response).isPresent();
        assertThat(response.get().externalTrackId()).isEqualTo("spotify-track-123");
        assertThat(response.get().sourceLabel()).isEqualTo("Recommended for you");
        assertThat(response.get().spotifyTrackId()).isEqualTo("spotify-track-123");
        assertThat(response.get().previewUrl()).isEqualTo("https://preview/123");
    }

    @Test
    void skipsGmsCandidatesMissingPreviewAndFallsBackToEditorial() {
        StoredSnapshot top = snapshotOf("rec-1", 1, "no-preview-track", "spotify", "Skipped", "Skipped Artist");
        when(snapshotStore.findRecentByUserId(eq("user-1"), anyInt())).thenReturn(List.of(top));
        when(trackRepository.findBySourcePlatformAndExternalTrackId("spotify", "no-preview-track"))
            .thenReturn(Optional.of(trackOf(
                "no-preview-track",
                "spotify",
                "spotify:track:no-preview-track",
                null,
                "Skipped",
                "Skipped Artist"
            )));
        when(trackRepository.findFirstByCollectionSourceAndPreviewUrlIsNotNullOrderByCollectedAtDesc("acquisition_pool"))
            .thenReturn(Optional.of(trackOf(
                "editorial-track-7",
                "spotify",
                "spotify:track:editorial-track-7",
                "https://preview/editorial",
                "Editorial Anthem",
                "Editor Artist"
            )));

        Optional<HeroTrackResponse> response = service.resolve("user-1");

        assertThat(response).isPresent();
        assertThat(response.get().externalTrackId()).isEqualTo("editorial-track-7");
        assertThat(response.get().sourceLabel()).isEqualTo("Editorial Pick");
        assertThat(response.get().previewUrl()).isEqualTo("https://preview/editorial");
    }

    @Test
    void anonymousUserGetsEditorialPickWithoutTouchingGmsStore() {
        when(trackRepository.findFirstByCollectionSourceAndPreviewUrlIsNotNullOrderByCollectedAtDesc("acquisition_pool"))
            .thenReturn(Optional.of(trackOf(
                "editorial-track-7",
                "spotify",
                "spotify:track:editorial-track-7",
                "https://preview/editorial",
                "Editorial Anthem",
                "Editor Artist"
            )));

        Optional<HeroTrackResponse> response = service.resolve(null);

        assertThat(response).isPresent();
        assertThat(response.get().sourceLabel()).isEqualTo("Editorial Pick");
    }

    @Test
    void blankUserIdIsTreatedAsAnonymous() {
        when(trackRepository.findFirstByCollectionSourceAndPreviewUrlIsNotNullOrderByCollectedAtDesc("acquisition_pool"))
            .thenReturn(Optional.of(trackOf(
                "editorial-track-7",
                "spotify",
                "spotify:track:editorial-track-7",
                "https://preview/editorial",
                "Editorial Anthem",
                "Editor Artist"
            )));

        Optional<HeroTrackResponse> response = service.resolve("   ");

        assertThat(response).isPresent();
        assertThat(response.get().externalTrackId()).isEqualTo("editorial-track-7");
    }

    @Test
    void fallsBackToAnyPreviewTrackWhenAcquisitionPoolEmpty() {
        when(snapshotStore.findRecentByUserId(anyString(), anyInt())).thenReturn(List.of());
        when(trackRepository.findFirstByCollectionSourceAndPreviewUrlIsNotNullOrderByCollectedAtDesc("acquisition_pool"))
            .thenReturn(Optional.empty());
        when(trackRepository.findFirstByPreviewUrlIsNotNullOrderByCollectedAtDesc())
            .thenReturn(Optional.of(trackOf(
                "search-track-9",
                "tidal",
                null,
                "https://preview/search",
                "Search Hit",
                "Search Artist"
            )));

        Optional<HeroTrackResponse> response = service.resolve("user-1");

        assertThat(response).isPresent();
        assertThat(response.get().externalTrackId()).isEqualTo("search-track-9");
        assertThat(response.get().sourceLabel()).isEqualTo("Editorial Pick");
    }

    @Test
    void returnsEmptyWhenNothingAvailable() {
        when(snapshotStore.findRecentByUserId(anyString(), anyInt())).thenReturn(List.of());
        when(trackRepository.findFirstByCollectionSourceAndPreviewUrlIsNotNullOrderByCollectedAtDesc("acquisition_pool"))
            .thenReturn(Optional.empty());
        when(trackRepository.findFirstByPreviewUrlIsNotNullOrderByCollectedAtDesc())
            .thenReturn(Optional.empty());

        Optional<HeroTrackResponse> response = service.resolve("user-1");

        assertThat(response).isEmpty();
    }

    private StoredSnapshot snapshotOf(
        String recommendationId,
        int rank,
        String candidateTrackId,
        String sourcePlatform,
        String title,
        String artistName
    ) {
        return new StoredSnapshot(
            1L,
            recommendationId,
            "req-" + recommendationId,
            "user-1",
            candidateTrackId,
            null,
            title,
            artistName,
            "gms",
            sourcePlatform,
            "model-v0",
            null,
            0.9,
            0.5,
            0.6,
            0.5,
            0.1,
            0.8,
            rank,
            "test",
            Instant.parse("2026-05-17T10:00:00Z")
        );
    }

    private EmsCollectedTrackEntity trackOf(
        String externalTrackId,
        String sourcePlatform,
        String spotifyUri,
        String previewUrl,
        String title,
        String artistName
    ) {
        return new EmsCollectedTrackEntity(
            externalTrackId,
            title,
            artistName,
            sourcePlatform,
            null,
            "Album",
            "https://image/" + externalTrackId,
            "https://platform/" + externalTrackId,
            spotifyUri,
            previewUrl,
            210_000,
            "acquisition_pool",
            Instant.parse("2026-05-17T09:00:00Z"),
            null
        );
    }
}
