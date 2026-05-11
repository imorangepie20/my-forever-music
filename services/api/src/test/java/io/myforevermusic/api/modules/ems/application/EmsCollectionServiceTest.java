package io.myforevermusic.api.modules.ems.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedPlaylistEntity;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedPlaylistRepository;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedPlaylistTrackEntity;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedPlaylistTrackRepository;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedTrackEntity;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedTrackRepository;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsTrackAudioFeatures;
import io.myforevermusic.api.modules.auth.application.AuthAccountStore;
import io.myforevermusic.api.modules.auth.application.AuthRegisteredAccount;
import io.myforevermusic.api.modules.platform.application.PlatformAccountCredential;
import io.myforevermusic.api.modules.platform.application.PlatformCredentialService;
import io.myforevermusic.api.modules.platform.infrastructure.reccobeats.ReccoBeatsAudioFeaturesClient;
import io.myforevermusic.api.modules.platform.infrastructure.reccobeats.ReccoBeatsAudioFeaturesClient.ReccoBeatsAudioFeaturesSnapshot;
import io.myforevermusic.api.modules.platform.infrastructure.reccobeats.ReccoBeatsAudioFeaturesClient.ReccoBeatsTrackLookupRequest;
import io.myforevermusic.api.modules.platform.infrastructure.spotify.SpotifyWebApiClient;
import io.myforevermusic.api.modules.platform.infrastructure.spotify.SpotifyWebApiClient.SpotifyPlaylistSummary;
import io.myforevermusic.api.modules.platform.infrastructure.spotify.SpotifyWebApiClient.SpotifyPlaylistTrack;
import io.myforevermusic.api.modules.platform.infrastructure.spotify.SpotifyWebApiClient.SpotifySearchResult;
import io.myforevermusic.api.modules.platform.infrastructure.tidal.TidalWebApiClient;
import io.myforevermusic.api.modules.platform.infrastructure.tidal.TidalWebApiClient.TidalSearchResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmsCollectionServiceTest {

    @Mock
    private SpotifyWebApiClient spotifyWebApiClient;

    @Mock
    private TidalWebApiClient tidalWebApiClient;

    @Mock
    private ReccoBeatsAudioFeaturesClient reccoBeatsAudioFeaturesClient;

    @Mock
    private PlatformCredentialService platformCredentialService;

    @Mock
    private AuthAccountStore authAccountStore;

    @Mock
    private EmsCollectedPlaylistRepository playlistRepository;

    @Mock
    private EmsCollectedTrackRepository trackRepository;

    @Mock
    private EmsCollectedPlaylistTrackRepository playlistTrackRepository;

    @Test
    void shouldPreviewSpotifySearchWithoutWritingToEmsStorage() {
        PlatformAccountCredential credential = credential("spotify");
        when(platformCredentialService.findUsableCredential("user-001", "spotify"))
            .thenReturn(Optional.of(credential));
        when(spotifyWebApiClient.searchPlaylists(credential, "vocal jazz"))
            .thenReturn(new SpotifySearchResult<>(List.of(
                new SpotifyPlaylistSummary(
                    "playlist-001",
                    "Vocal Jazz",
                    "Stored only after explicit collection",
                    "owner-001",
                    "Curator",
                    false,
                    12,
                    null,
                    "https://open.spotify.com/playlist/playlist-001",
                    "spotify:playlist:playlist-001"
                )
            ), 1));
        when(spotifyWebApiClient.searchTracks(credential, "vocal jazz"))
            .thenReturn(new SpotifySearchResult<>(List.of(
                new SpotifyPlaylistTrack(
                    "track-001",
                    "Search Preview Track",
                    "Preview Artist",
                    "Preview Album",
                    null,
                    null,
                    "https://open.spotify.com/track/track-001",
                    "spotify:track:track-001",
                    null,
                    null,
                    180000
                )
            ), 1));

        EmsCollectionService service = service();
        EmsCollectionService.EmsCollectionSearchPreviewResult result =
            service.previewSearch("user-001", "spotify", "vocal jazz");

        assertThat(result.resultPlaylistCount()).isEqualTo(1);
        assertThat(result.resultTrackCount()).isEqualTo(1);
        assertThat(result.playlists()).extracting(EmsCollectionService.EmsCollectionSearchPlaylistPreview::title)
            .containsExactly("Vocal Jazz");
        assertThat(result.tracks()).extracting(EmsCollectionService.EmsCollectionSearchTrackPreview::title)
            .containsExactly("Search Preview Track");
        verifyNoInteractions(playlistRepository, trackRepository, playlistTrackRepository, reccoBeatsAudioFeaturesClient);
    }

    @Test
    void shouldPreviewPreferredProviderSearchWithoutCrossProviderLookup() {
        PlatformAccountCredential tidalCredential = credential("tidal");
        when(authAccountStore.findByUserId("user-001"))
            .thenReturn(Optional.of(account("tidal")));
        when(platformCredentialService.findUsableCredential("user-001", "tidal"))
            .thenReturn(Optional.of(tidalCredential));
        when(tidalWebApiClient.searchPlaylistResults(tidalCredential, "jazz"))
            .thenReturn(new TidalSearchResult<>(List.of(new io.myforevermusic.api.modules.platform.infrastructure.tidal.TidalWebApiClient.TidalPlaylistSummary(
                "tidal-playlist-001",
                "TIDAL Jazz",
                "TIDAL preview",
                20,
                null,
                null,
                "https://tidal.com/browse/playlist/tidal-playlist-001",
                "tidal-playlist-001"
            )), 7));
        when(tidalWebApiClient.searchTrackResults(tidalCredential, "jazz"))
            .thenReturn(new TidalSearchResult<>(List.of(new io.myforevermusic.api.modules.platform.infrastructure.tidal.TidalWebApiClient.TidalPlaylistTrack(
                "tidal-track-001",
                "TIDAL Track",
                "TIDAL Artist",
                "TIDAL Album",
                null,
                "https://tidal.com/browse/track/tidal-track-001",
                "tidal:track:tidal-track-001",
                null,
                "USRC17607839",
                180000
            )), 99));

        EmsCollectionService.EmsCollectionSearchPreviewResult result =
            service().previewSearch("user-001", null, "jazz");

        assertThat(result.platformId()).isEqualTo("tidal");
        assertThat(result.resultPlaylistCount()).isEqualTo(7);
        assertThat(result.resultTrackCount()).isEqualTo(99);
        assertThat(result.playlists()).extracting(EmsCollectionService.EmsCollectionSearchPlaylistPreview::sourcePlatform)
            .containsExactly("tidal");
        assertThat(result.tracks()).extracting(EmsCollectionService.EmsCollectionSearchTrackPreview::sourcePlatform)
            .containsExactly("tidal");
        verifyNoInteractions(spotifyWebApiClient);
        verifyNoInteractions(playlistRepository, trackRepository, playlistTrackRepository, reccoBeatsAudioFeaturesClient);
    }

    @Test
    void shouldBackfillTidalPlaylistAudioFeaturesByIsrc() {
        EmsCollectedPlaylistEntity playlist = new EmsCollectedPlaylistEntity(
            "playlist-001",
            "Pop Hits",
            "tidal",
            "",
            "",
            null,
            "https://tidal.com/browse/playlist/playlist-001",
            null,
            1,
            "tidal_home_page",
            "THE_HITS",
            Instant.parse("2026-05-10T00:00:00Z")
        );
        ReflectionTestUtils.setField(playlist, "id", 2L);
        EmsCollectedTrackEntity track = new EmsCollectedTrackEntity(
            "tidal-track-001",
            "Midnight Receiver",
            "Neon Bloom",
            "tidal",
            "USRC17607839",
            "Album",
            null,
            "https://tidal.com/browse/track/tidal-track-001",
            "tidal:track:tidal-track-001",
            null,
            218000,
            "tidal_home_page",
            Instant.parse("2026-05-10T00:00:00Z"),
            unavailableAudioFeatures()
        );
        EmsCollectedPlaylistTrackEntity link = new EmsCollectedPlaylistTrackEntity(playlist, track, 0);

        when(playlistRepository.findById(2L)).thenReturn(Optional.of(playlist));
        when(playlistTrackRepository.findByPlaylistIdOrderBySortOrderAsc(2L)).thenReturn(List.of(link));
        when(reccoBeatsAudioFeaturesClient.getAudioFeaturesForExternalTracksByIsrc(List.of(
            new ReccoBeatsTrackLookupRequest(
                "tidal-track-001",
                "Midnight Receiver",
                "Neon Bloom",
                218000,
                "USRC17607839"
            )
        ))).thenReturn(Map.of(
            "tidal-track-001",
            new ReccoBeatsAudioFeaturesSnapshot(
                "spotify-track-001",
                "recco-track-001",
                "https://open.spotify.com/track/spotify-track-001",
                "USRC17607839",
                0.211,
                0.702,
                0.744,
                0.013,
                8,
                0.094,
                -8.7,
                1,
                0.039,
                118.4,
                0.58,
                Instant.parse("2026-05-10T01:00:00Z")
            )
        ));

        EmsCollectionService.EmsAudioFeatureBackfillResult result = service()
            .backfillAudioFeaturesForPlaylist(2L);

        assertThat(result.newlyFilledTrackCount()).isEqualTo(1);
        assertThat(result.missingIsrcTrackCount()).isZero();
        assertThat(result.coverageRatioAfter()).isEqualTo(1.0);
        assertThat(track.getAudioFeatures().isAudioFeaturesFilled()).isTrue();
        assertThat(track.getAudioFeatures().getAudioFeatureSource()).isEqualTo("reccobeats_isrc_match");
        assertThat(track.getAudioFeatures().getTempo()).isEqualTo(118.4);
        verify(trackRepository).saveAll(List.of(track));
    }

    @Test
    void shouldReportTidalBackfillTracksMissingIsrc() {
        EmsCollectedPlaylistEntity playlist = new EmsCollectedPlaylistEntity(
            "playlist-002",
            "No ISRC",
            "tidal",
            "",
            "",
            null,
            "https://tidal.com/browse/playlist/playlist-002",
            null,
            1,
            "tidal_home_page",
            "THE_HITS",
            Instant.parse("2026-05-10T00:00:00Z")
        );
        ReflectionTestUtils.setField(playlist, "id", 3L);
        EmsCollectedTrackEntity track = new EmsCollectedTrackEntity(
            "tidal-track-002",
            "Missing Code",
            "Stored Artist",
            "tidal",
            null,
            "Album",
            null,
            "https://tidal.com/browse/track/tidal-track-002",
            "tidal:track:tidal-track-002",
            null,
            218000,
            "tidal_home_page",
            Instant.parse("2026-05-10T00:00:00Z"),
            unavailableAudioFeatures()
        );
        EmsCollectedPlaylistTrackEntity link = new EmsCollectedPlaylistTrackEntity(playlist, track, 0);

        when(playlistRepository.findById(3L)).thenReturn(Optional.of(playlist));
        when(playlistTrackRepository.findByPlaylistIdOrderBySortOrderAsc(3L)).thenReturn(List.of(link));

        EmsCollectionService.EmsAudioFeatureBackfillResult result = service()
            .backfillAudioFeaturesForPlaylist(3L);

        assertThat(result.eligibleTrackCount()).isZero();
        assertThat(result.missingIsrcTrackCount()).isEqualTo(1);
        assertThat(result.matchedSnapshotCount()).isZero();
        assertThat(result.newlyFilledTrackCount()).isZero();
        verifyNoInteractions(trackRepository, reccoBeatsAudioFeaturesClient);
    }

    private EmsCollectionService service() {
        return new EmsCollectionService(
            spotifyWebApiClient,
            tidalWebApiClient,
            reccoBeatsAudioFeaturesClient,
            platformCredentialService,
            authAccountStore,
            playlistRepository,
            trackRepository,
            playlistTrackRepository
        );
    }

    private AuthRegisteredAccount account(String preferredPlatformId) {
        return new AuthRegisteredAccount(
            "user-001",
            "user@example.com",
            "user@example.com",
            "User",
            preferredPlatformId,
            null,
            null,
            false,
            "registered",
            Instant.parse("2026-05-09T00:00:00Z"),
            Instant.parse("2026-05-09T00:00:00Z"),
            Instant.parse("2026-05-09T00:00:00Z")
        );
    }

    private PlatformAccountCredential credential(String platformId) {
        return new PlatformAccountCredential(
            "user-001",
            platformId,
            "oauth",
            "external-user-001",
            "External User",
            "access-token",
            "refresh-token",
            "Bearer",
            "playlist-read",
            Instant.parse("2026-05-10T00:00:00Z"),
            Instant.parse("2026-05-09T00:00:00Z"),
            Instant.parse("2026-05-09T00:00:00Z")
        );
    }

    private EmsTrackAudioFeatures unavailableAudioFeatures() {
        return new EmsTrackAudioFeatures(
            null,
            "unavailable",
            false,
            null,
            null,
            null,
            "audio_features",
            218000,
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
            null,
            Instant.parse("2026-05-10T00:00:00Z")
        );
    }
}
