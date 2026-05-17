package io.myforevermusic.api.modules.mainpage.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.data.domain.Pageable;

class HeroTrackServiceTest {

    private final RecommendationSnapshotStore snapshotStore = mock(RecommendationSnapshotStore.class);
    private final EmsCollectedTrackRepository trackRepository = mock(EmsCollectedTrackRepository.class);
    private final HeroTrackService service = new HeroTrackService(snapshotStore, trackRepository);

    @Test
    void returnsTopGmsRecommendationWhenUserHasSnapshot() {
        StoredSnapshot top = snapshotOf("rec-1", 1, "spotify-track-123", "spotify");
        StoredSnapshot lower = snapshotOf("rec-2", 4, "spotify-track-456", "spotify");
        when(snapshotStore.findRecentByUserId(eq("user-1"), anyInt())).thenReturn(List.of(lower, top));
        when(trackRepository.findBySourcePlatformAndExternalTrackId("spotify", "spotify-track-123"))
            .thenReturn(Optional.of(trackOf("spotify-track-123", "spotify", "https://preview/123")));
        when(trackRepository.findBySourcePlatformAndExternalTrackId("spotify", "spotify-track-456"))
            .thenReturn(Optional.of(trackOf("spotify-track-456", "spotify", "https://preview/456")));

        Optional<HeroTrackResponse> response = service.resolve("user-1");

        assertThat(response).isPresent();
        assertThat(response.get().externalTrackId()).isEqualTo("spotify-track-123");
        assertThat(response.get().sourceLabel()).isEqualTo("Recommended for you");
    }

    @Test
    void resolveListMixesGmsAndEmsUpToLimit() {
        StoredSnapshot top = snapshotOf("rec-1", 1, "gms-track-1", "spotify");
        when(snapshotStore.findRecentByUserId(eq("user-1"), anyInt())).thenReturn(List.of(top));
        when(trackRepository.findBySourcePlatformAndExternalTrackId("spotify", "gms-track-1"))
            .thenReturn(Optional.of(trackOf("gms-track-1", "spotify", "https://preview/gms")));
        when(trackRepository.findRecentByCollectionSourceWithPreview(eq("acquisition_pool"), any(Pageable.class)))
            .thenReturn(List.of(
                trackOf("ems-track-1", "spotify", "https://preview/ems-1"),
                trackOf("ems-track-2", "spotify", "https://preview/ems-2"),
                trackOf("ems-track-3", "spotify", "https://preview/ems-3"),
                trackOf("ems-track-4", "spotify", "https://preview/ems-4")
            ));

        List<HeroTrackResponse> list = service.resolveList("user-1", 5);

        assertThat(list).hasSize(5);
        assertThat(list.get(0).externalTrackId()).isEqualTo("gms-track-1");
        assertThat(list.get(0).sourceLabel()).isEqualTo("Recommended for you");
        assertThat(list).extracting(HeroTrackResponse::externalTrackId)
            .containsExactlyInAnyOrder("gms-track-1", "ems-track-1", "ems-track-2", "ems-track-3", "ems-track-4");
    }

    @Test
    void resolveListSkipsDuplicateBetweenGmsAndEms() {
        StoredSnapshot top = snapshotOf("rec-1", 1, "shared-track", "spotify");
        when(snapshotStore.findRecentByUserId(eq("user-1"), anyInt())).thenReturn(List.of(top));
        when(trackRepository.findBySourcePlatformAndExternalTrackId("spotify", "shared-track"))
            .thenReturn(Optional.of(trackOf("shared-track", "spotify", "https://preview/shared")));
        when(trackRepository.findRecentByCollectionSourceWithPreview(eq("acquisition_pool"), any(Pageable.class)))
            .thenReturn(List.of(
                trackOf("shared-track", "spotify", "https://preview/shared"),
                trackOf("ems-track-1", "spotify", "https://preview/ems-1")
            ));

        List<HeroTrackResponse> list = service.resolveList("user-1", 5);

        assertThat(list).hasSize(2);
        assertThat(list).extracting(HeroTrackResponse::externalTrackId)
            .containsExactlyInAnyOrder("shared-track", "ems-track-1");
    }

    @Test
    void skipsGmsCandidatesMissingPreviewAndFallsBackToEditorial() {
        StoredSnapshot top = snapshotOf("rec-1", 1, "no-preview-track", "spotify");
        when(snapshotStore.findRecentByUserId(eq("user-1"), anyInt())).thenReturn(List.of(top));
        when(trackRepository.findBySourcePlatformAndExternalTrackId("spotify", "no-preview-track"))
            .thenReturn(Optional.of(trackOf("no-preview-track", "spotify", null)));
        when(trackRepository.findRecentByCollectionSourceWithPreview(eq("acquisition_pool"), any(Pageable.class)))
            .thenReturn(List.of(trackOf("editorial-track", "spotify", "https://preview/editorial")));

        Optional<HeroTrackResponse> response = service.resolve("user-1");

        assertThat(response).isPresent();
        assertThat(response.get().externalTrackId()).isEqualTo("editorial-track");
        assertThat(response.get().sourceLabel()).isEqualTo("Editorial Pick");
    }

    @Test
    void anonymousUserGetsEditorialPickWithoutTouchingGmsStore() {
        when(trackRepository.findRecentByCollectionSourceWithPreview(eq("acquisition_pool"), any(Pageable.class)))
            .thenReturn(List.of(trackOf("editorial-track", "spotify", "https://preview/editorial")));

        Optional<HeroTrackResponse> response = service.resolve(null);

        assertThat(response).isPresent();
        assertThat(response.get().sourceLabel()).isEqualTo("Editorial Pick");
    }

    @Test
    void blankUserIdIsTreatedAsAnonymous() {
        when(trackRepository.findRecentByCollectionSourceWithPreview(eq("acquisition_pool"), any(Pageable.class)))
            .thenReturn(List.of(trackOf("editorial-track", "spotify", "https://preview/editorial")));

        Optional<HeroTrackResponse> response = service.resolve("   ");

        assertThat(response).isPresent();
        assertThat(response.get().externalTrackId()).isEqualTo("editorial-track");
    }

    @Test
    void fallsBackToAnyPreviewTrackWhenAcquisitionPoolEmpty() {
        when(snapshotStore.findRecentByUserId(anyString(), anyInt())).thenReturn(List.of());
        when(trackRepository.findRecentByCollectionSourceWithPreview(eq("acquisition_pool"), any(Pageable.class)))
            .thenReturn(List.of());
        when(trackRepository.findRecentWithPreview(any(Pageable.class)))
            .thenReturn(List.of(trackOf("search-track", "tidal", "https://preview/search")));

        Optional<HeroTrackResponse> response = service.resolve("user-1");

        assertThat(response).isPresent();
        assertThat(response.get().externalTrackId()).isEqualTo("search-track");
    }

    @Test
    void returnsEmptyWhenNothingAvailable() {
        when(snapshotStore.findRecentByUserId(anyString(), anyInt())).thenReturn(List.of());
        when(trackRepository.findRecentByCollectionSourceWithPreview(eq("acquisition_pool"), any(Pageable.class)))
            .thenReturn(List.of());
        when(trackRepository.findRecentWithPreview(any(Pageable.class)))
            .thenReturn(List.of());

        Optional<HeroTrackResponse> response = service.resolve("user-1");

        assertThat(response).isEmpty();
    }

    @Test
    void resolveListReturnsEmptyListWhenNoCandidates() {
        when(snapshotStore.findRecentByUserId(anyString(), anyInt())).thenReturn(List.of());
        when(trackRepository.findRecentByCollectionSourceWithPreview(eq("acquisition_pool"), any(Pageable.class)))
            .thenReturn(List.of());
        when(trackRepository.findRecentWithPreview(any(Pageable.class)))
            .thenReturn(List.of());

        List<HeroTrackResponse> list = service.resolveList("user-1", 5);

        assertThat(list).isEmpty();
    }

    private StoredSnapshot snapshotOf(String recommendationId, int rank, String candidateTrackId, String sourcePlatform) {
        return new StoredSnapshot(
            1L,
            recommendationId,
            "req-" + recommendationId,
            "user-1",
            candidateTrackId,
            null,
            "Title",
            "Artist",
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

    private EmsCollectedTrackEntity trackOf(String externalTrackId, String sourcePlatform, String previewUrl) {
        return new EmsCollectedTrackEntity(
            externalTrackId,
            "Title " + externalTrackId,
            "Artist " + externalTrackId,
            sourcePlatform,
            null,
            "Album",
            "https://image/" + externalTrackId,
            "https://platform/" + externalTrackId,
            "spotify".equals(sourcePlatform) ? "spotify:track:" + externalTrackId : null,
            previewUrl,
            210_000,
            "acquisition_pool",
            Instant.parse("2026-05-17T09:00:00Z"),
            null
        );
    }
}
