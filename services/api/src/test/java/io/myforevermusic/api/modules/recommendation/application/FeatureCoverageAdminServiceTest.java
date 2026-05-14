package io.myforevermusic.api.modules.recommendation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.myforevermusic.api.modules.auth.application.AuthAccountStore;
import io.myforevermusic.api.modules.auth.application.AuthRegisteredAccount;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedTrackRepository;
import io.myforevermusic.api.modules.pms.application.PmsUserLibraryStore;
import io.myforevermusic.api.modules.pms.infrastructure.persistence.PmsTrackAudioFeatures;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class FeatureCoverageAdminServiceTest {

    @Test
    void shouldSummarizeRecommendationFeatureCoverageForTargetUser() {
        AuthAccountStore authAccountStore = mock(AuthAccountStore.class);
        PmsUserLibraryStore pmsUserLibraryStore = mock(PmsUserLibraryStore.class);
        UserMusicEventStore eventStore = mock(UserMusicEventStore.class);
        RecommendationSnapshotStore snapshotStore = mock(RecommendationSnapshotStore.class);
        EmsCollectedTrackRepository emsTrackRepository = mock(EmsCollectedTrackRepository.class);

        when(authAccountStore.findByUserId("admin-user")).thenReturn(Optional.of(adminAccount("admin-user")));
        when(pmsUserLibraryStore.findPlaylists("target-user")).thenReturn(List.of(new PmsUserLibraryStore.LibraryPlaylistState(
            "target-user",
            "playlist-001",
            "external-playlist-001",
            "Target Playlist",
            "spotify",
            "curator",
            null,
            null,
            null,
            null,
            Instant.parse("2026-05-14T00:00:00Z"),
            List.of(
                track("track-001", "spotify", "USRC17607839", "spotify-001", null, "native", completeFeatures()),
                track("track-002", "tidal", "GBAYE0601498", null, "tidal-002", "resolved", PmsTrackAudioFeatures.unresolved()),
                track("track-003", "soundcloud", null, null, null, "unresolved", null)
            )
        )));
        when(eventStore.countEventsByUserIdAfter("target-user", Instant.EPOCH)).thenReturn(7L);
        when(snapshotStore.findRecentByUserId("target-user", 1000)).thenReturn(List.of(snapshot(1L), snapshot(2L)));
        when(emsTrackRepository.summarizeFeatureCoverageBySourcePlatform()).thenReturn(List.of(
            new EmsCoverageRow("spotify", 10L, 8L, 9L, 6L),
            new EmsCoverageRow("tidal", 5L, 2L, 5L, 1L)
        ));

        FeatureCoverageAdminService.FeatureCoverageReport report = new FeatureCoverageAdminService(
            authAccountStore,
            pmsUserLibraryStore,
            eventStore,
            snapshotStore,
            Optional.of(emsTrackRepository)
        ).summarize("admin-user", "target-user");

        assertThat(report.status()).isEqualTo("ok");
        assertThat(report.targetUserId()).isEqualTo("target-user");
        assertThat(report.pmsLibrary().playlistCount()).isEqualTo(1);
        assertThat(report.pmsLibrary().trackCount()).isEqualTo(3);
        assertThat(report.pmsLibrary().audioFeatureFilledCount()).isEqualTo(1);
        assertThat(report.pmsLibrary().audioFeatureCoverageRatio()).isEqualTo(0.3333d);
        assertThat(report.pmsLibrary().isrcCount()).isEqualTo(2);
        assertThat(report.pmsLibrary().playbackTargetAvailableCount()).isEqualTo(2);
        assertThat(report.emsPool().trackCount()).isEqualTo(15);
        assertThat(report.emsPool().audioFeatureFilledCount()).isEqualTo(10);
        assertThat(report.emsPool().audioFeatureCoverageRatio()).isEqualTo(0.6667d);
        assertThat(report.emsPool().isrcCount()).isEqualTo(14);
        assertThat(report.emsPool().canonicalTrackCount()).isEqualTo(7);
        assertThat(report.emsPool().sources()).extracting(FeatureCoverageAdminService.EmsSourceCoverage::sourcePlatform)
            .containsExactly("spotify", "tidal");
        assertThat(report.learningData().eventCount()).isEqualTo(7);
        assertThat(report.learningData().recentRecommendationSnapshotCount()).isEqualTo(2);
        assertThat(report.warnings()).isEmpty();
    }

    @Test
    void shouldReportDegradedCoverageWhenEmsRepositoryIsUnavailable() {
        AuthAccountStore authAccountStore = mock(AuthAccountStore.class);
        PmsUserLibraryStore pmsUserLibraryStore = mock(PmsUserLibraryStore.class);
        UserMusicEventStore eventStore = mock(UserMusicEventStore.class);
        RecommendationSnapshotStore snapshotStore = mock(RecommendationSnapshotStore.class);

        when(authAccountStore.findByUserId("admin-user")).thenReturn(Optional.of(adminAccount("admin-user")));
        when(pmsUserLibraryStore.findPlaylists("admin-user")).thenReturn(List.of());
        when(eventStore.countEventsByUserIdAfter("admin-user", Instant.EPOCH)).thenReturn(0L);
        when(snapshotStore.findRecentByUserId("admin-user", 1000)).thenReturn(List.of());

        FeatureCoverageAdminService.FeatureCoverageReport report = new FeatureCoverageAdminService(
            authAccountStore,
            pmsUserLibraryStore,
            eventStore,
            snapshotStore,
            Optional.empty()
        ).summarize("admin-user", null);

        assertThat(report.status()).isEqualTo("degraded");
        assertThat(report.emsPool().warnings()).hasSize(1);
        assertThat(report.warnings()).containsExactlyElementsOf(report.emsPool().warnings());
    }

    @Test
    void shouldRejectNonAdminUsers() {
        AuthAccountStore authAccountStore = mock(AuthAccountStore.class);
        when(authAccountStore.findByUserId("user-001")).thenReturn(Optional.of(userAccount("user-001")));

        FeatureCoverageAdminService service = new FeatureCoverageAdminService(
            authAccountStore,
            mock(PmsUserLibraryStore.class),
            mock(UserMusicEventStore.class),
            mock(RecommendationSnapshotStore.class),
            Optional.empty()
        );

        assertThatThrownBy(() -> service.summarize("user-001", null))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403 FORBIDDEN");
    }

    private AuthRegisteredAccount adminAccount(String userId) {
        return account(userId, "jowoosungtidal@gmail.com");
    }

    private AuthRegisteredAccount userAccount(String userId) {
        return account(userId, "user@example.com");
    }

    private AuthRegisteredAccount account(String userId, String normalizedEmail) {
        return new AuthRegisteredAccount(
            userId,
            normalizedEmail,
            normalizedEmail,
            "Test User",
            "spotify",
            null,
            null,
            false,
            "complete",
            Instant.parse("2026-05-14T00:00:00Z"),
            Instant.parse("2026-05-14T00:00:00Z"),
            Instant.parse("2026-05-14T00:00:00Z")
        );
    }

    private PmsUserLibraryStore.LibraryTrackState track(
        String trackId,
        String sourcePlatform,
        String isrc,
        String spotifyTrackId,
        String tidalTrackId,
        String playbackTargetStatus,
        PmsTrackAudioFeatures audioFeatures
    ) {
        return new PmsUserLibraryStore.LibraryTrackState(
            trackId,
            "external-" + trackId,
            "Track " + trackId,
            "Artist",
            sourcePlatform,
            null,
            null,
            null,
            null,
            null,
            null,
            isrc,
            spotifyTrackId,
            null,
            tidalTrackId,
            null,
            null,
            playbackTargetStatus,
            1,
            false,
            audioFeatures
        );
    }

    private PmsTrackAudioFeatures completeFeatures() {
        return new PmsTrackAudioFeatures(
            "spotify-001",
            "reccobeats_spotify_track",
            true,
            null,
            null,
            "spotify:track:001",
            "audio_features",
            180000,
            5,
            1,
            4,
            0.12d,
            0.72d,
            0.81d,
            0.01d,
            0.08d,
            -6.0d,
            0.04d,
            120.0d,
            0.64d,
            Instant.parse("2026-05-14T00:00:00Z")
        );
    }

    private RecommendationSnapshotStore.StoredSnapshot snapshot(Long id) {
        return new RecommendationSnapshotStore.StoredSnapshot(
            id,
            "recommendation-001",
            "request-001",
            "target-user",
            "track-001",
            null,
            "Track",
            "Artist",
            "gms",
            "spotify",
            "gms-baseline-v1",
            null,
            0.8d,
            0.5d,
            0.9d,
            0.7d,
            0.1d,
            0.95d,
            id.intValue(),
            "reason",
            Instant.parse("2026-05-14T00:00:00Z")
        );
    }

    private record EmsCoverageRow(
        String sourcePlatform,
        Long trackCount,
        Long audioFeatureFilledCount,
        Long isrcCount,
        Long canonicalTrackCount
    ) implements EmsCollectedTrackRepository.FeatureCoverageBySourcePlatform {

        @Override
        public String getSourcePlatform() {
            return sourcePlatform;
        }

        @Override
        public Long getTrackCount() {
            return trackCount;
        }

        @Override
        public Long getAudioFeatureFilledCount() {
            return audioFeatureFilledCount;
        }

        @Override
        public Long getIsrcCount() {
            return isrcCount;
        }

        @Override
        public Long getCanonicalTrackCount() {
            return canonicalTrackCount;
        }
    }
}
