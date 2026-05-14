package io.myforevermusic.api.modules.recommendation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.myforevermusic.api.modules.auth.application.AuthAccountStore;
import io.myforevermusic.api.modules.auth.application.AuthRegisteredAccount;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedTrackEntity;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedTrackRepository;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsTrackAudioFeatures;
import io.myforevermusic.api.modules.gms.presentation.GmsRecommendationPreviewResponse.RecommendationItem;
import io.myforevermusic.api.modules.pms.application.PmsUserLibraryStore.LibraryPlaylistState;
import io.myforevermusic.api.modules.pms.application.PmsUserLibraryStore.LibraryTrackState;
import io.myforevermusic.api.modules.pms.infrastructure.local.InMemoryPmsUserLibraryStore;
import io.myforevermusic.api.modules.pms.infrastructure.persistence.PmsTrackAudioFeatures;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ColdStartFallbackServiceTest {

    private static final String USER_ID = "user-001";

    private AuthAccountStore authAccountStore;
    private InMemoryPmsUserLibraryStore pmsUserLibraryStore;
    private EmsCollectedTrackRepository emsTrackRepository;
    private ColdStartFallbackService service;

    @BeforeEach
    void setUp() {
        authAccountStore = mock(AuthAccountStore.class);
        pmsUserLibraryStore = new InMemoryPmsUserLibraryStore();
        emsTrackRepository = mock(EmsCollectedTrackRepository.class);
        service = new ColdStartFallbackService(
            authAccountStore,
            pmsUserLibraryStore,
            Optional.of(emsTrackRepository)
        );
        ReflectionTestUtils.setField(service, "fallbackLimit", 5);
    }

    @Test
    void isColdStartReturnsTrueWhenPmsLibraryEmpty() {
        assertThat(service.isColdStart(USER_ID)).isTrue();
    }

    @Test
    void isColdStartReturnsFalseWhenAnyPlaylistHasTracks() {
        pmsUserLibraryStore.savePlaylists(USER_ID, List.of(playlistWithTracks(1)));
        assertThat(service.isColdStart(USER_ID)).isFalse();
    }

    @Test
    void isColdStartReturnsFalseWhenUserIdMissing() {
        assertThat(service.isColdStart(null)).isFalse();
        assertThat(service.isColdStart("  ")).isFalse();
    }

    @Test
    void fallbackItemsPrefersAudioFeatureFilledTracksFromPreferredPlatform() {
        when(authAccountStore.findByUserId(USER_ID)).thenReturn(Optional.of(account("tidal")));
        EmsCollectedTrackEntity withAudio = trackWithId(11L, "Filled", "Artist A", "tidal", true);
        EmsCollectedTrackEntity withoutAudio = trackWithId(12L, "Empty", "Artist B", "tidal", false);
        when(emsTrackRepository.findBySourcePlatformOrderByCollectedAtDesc("tidal"))
            .thenReturn(List.of(withoutAudio, withAudio));

        List<RecommendationItem> items = service.fallbackItems(USER_ID, null);

        assertThat(items).extracting(RecommendationItem::trackId)
            .containsExactly("ems-11", "ems-12");
        assertThat(items).allSatisfy(item -> {
            assertThat(item.sourceSpace()).isEqualTo("cold_start");
            assertThat(item.reason()).isEqualTo("Cold-start fallback from EMS pool");
        });
    }

    @Test
    void fallbackItemsFallsBackToAnotherPlatformWhenPreferredEmpty() {
        when(authAccountStore.findByUserId(USER_ID)).thenReturn(Optional.of(account("apple-music")));
        when(emsTrackRepository.findBySourcePlatformOrderByCollectedAtDesc("apple-music"))
            .thenReturn(List.of());
        when(emsTrackRepository.findDistinctSourcePlatforms()).thenReturn(List.of("apple-music", "spotify"));
        when(emsTrackRepository.findBySourcePlatformOrderByCollectedAtDesc("spotify"))
            .thenReturn(List.of(trackWithId(21L, "Spotify Pick", "Artist S", "spotify", true)));

        List<RecommendationItem> items = service.fallbackItems(USER_ID, null);

        assertThat(items).extracting(RecommendationItem::trackId).containsExactly("ems-21");
    }

    @Test
    void fallbackItemsReturnsEmptyWhenEmsRepositoryUnavailable() {
        ColdStartFallbackService noEms = new ColdStartFallbackService(
            authAccountStore,
            pmsUserLibraryStore,
            Optional.empty()
        );
        assertThat(noEms.fallbackItems(USER_ID, null)).isEmpty();
    }

    @Test
    void fallbackItemsRespectsExplicitLimitOverride() {
        when(authAccountStore.findByUserId(USER_ID)).thenReturn(Optional.of(account("tidal")));
        when(emsTrackRepository.findBySourcePlatformOrderByCollectedAtDesc("tidal"))
            .thenReturn(List.of(
                trackWithId(1L, "T1", "A1", "tidal", true),
                trackWithId(2L, "T2", "A2", "tidal", true),
                trackWithId(3L, "T3", "A3", "tidal", true)
            ));

        List<RecommendationItem> items = service.fallbackItems(USER_ID, 2);

        assertThat(items).extracting(RecommendationItem::trackId).containsExactly("ems-1", "ems-2");
    }

    private AuthRegisteredAccount account(String preferredPlatformId) {
        return new AuthRegisteredAccount(
            USER_ID,
            "u@example.com",
            "u@example.com",
            "User",
            preferredPlatformId,
            null,
            null,
            false,
            "ready",
            Instant.parse("2026-05-01T00:00:00Z"),
            Instant.parse("2026-05-01T00:00:00Z"),
            Instant.parse("2026-05-01T00:00:00Z")
        );
    }

    private EmsCollectedTrackEntity trackWithId(long id, String title, String artist, String platform, boolean filled) {
        EmsTrackAudioFeatures features = new EmsTrackAudioFeatures(
            "audio-" + id,
            "reccobeats",
            filled,
            null, null, null, null,
            null, null, null, null,
            null, null, null, null,
            null, null, null, null, null,
            Instant.parse("2026-05-14T00:00:00Z")
        );
        EmsCollectedTrackEntity entity = new EmsCollectedTrackEntity(
            "external-" + id,
            title,
            artist,
            platform,
            null,
            "Album " + id,
            null,
            null,
            null,
            null,
            210000,
            "search_pool",
            Instant.parse("2026-05-14T00:00:00Z"),
            features
        );
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    private LibraryPlaylistState playlistWithTracks(int count) {
        List<LibraryTrackState> tracks = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            tracks.add(new LibraryTrackState(
                "track-" + i,
                "external-" + i,
                "Title " + i,
                "Artist",
                "tidal",
                null,
                "Album",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                i,
                false,
                (PmsTrackAudioFeatures) null
            ));
        }
        return new LibraryPlaylistState(
            USER_ID,
            "playlist-1",
            "external-playlist-1",
            "Playlist",
            "tidal",
            "Curator",
            null,
            null,
            null,
            null,
            Instant.parse("2026-05-14T00:00:00Z"),
            tracks
        );
    }
}
