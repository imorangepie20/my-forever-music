package io.myforevermusic.api.modules.ems.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedPlaylistRepository;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedPlaylistTrackRepository;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedTrackRepository;
import io.myforevermusic.api.modules.platform.application.PlatformAccountCredential;
import io.myforevermusic.api.modules.platform.application.PlatformCredentialService;
import io.myforevermusic.api.modules.platform.infrastructure.reccobeats.ReccoBeatsAudioFeaturesClient;
import io.myforevermusic.api.modules.platform.infrastructure.spotify.SpotifyWebApiClient;
import io.myforevermusic.api.modules.platform.infrastructure.spotify.SpotifyWebApiClient.SpotifyPlaylistSummary;
import io.myforevermusic.api.modules.platform.infrastructure.spotify.SpotifyWebApiClient.SpotifyPlaylistTrack;
import io.myforevermusic.api.modules.platform.infrastructure.spotify.SpotifyWebApiClient.SpotifySearchResult;
import io.myforevermusic.api.modules.platform.infrastructure.tidal.TidalWebApiClient;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        when(spotifyWebApiClient.searchPlaylists(credential, "vocal jazz", 5))
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
        when(spotifyWebApiClient.searchTracks(credential, "vocal jazz", 5))
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
            service.previewSearch("user-001", "spotify", "vocal jazz", 5);

        assertThat(result.resultPlaylistCount()).isEqualTo(1);
        assertThat(result.resultTrackCount()).isEqualTo(1);
        verifyNoInteractions(playlistRepository, trackRepository, playlistTrackRepository, reccoBeatsAudioFeaturesClient);
    }

    private EmsCollectionService service() {
        return new EmsCollectionService(
            spotifyWebApiClient,
            tidalWebApiClient,
            reccoBeatsAudioFeaturesClient,
            platformCredentialService,
            playlistRepository,
            trackRepository,
            playlistTrackRepository
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
}
