package io.myforevermusic.api.modules.melon.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.myforevermusic.api.common.error.ApiResourceNotFoundException;
import io.myforevermusic.api.modules.melon.infrastructure.persistence.MelonChartTrackEntity;
import io.myforevermusic.api.modules.melon.infrastructure.persistence.MelonChartTrackRepository;
import io.myforevermusic.api.modules.melon.presentation.MelonResolveResponse;
import io.myforevermusic.api.modules.platform.application.PlatformAccountCredential;
import io.myforevermusic.api.modules.platform.application.PlatformCredentialService;
import io.myforevermusic.api.modules.platform.application.TidalPlaybackTargetResolverService;
import io.myforevermusic.api.modules.platform.application.TidalPlaybackTargetResolverService.TidalPlaybackTarget;
import io.myforevermusic.api.modules.platform.application.TidalPlaybackTargetResolverService.TrackQuery;
import io.myforevermusic.api.modules.platform.infrastructure.spotify.SpotifyPublicCatalogClient;
import io.myforevermusic.api.modules.platform.infrastructure.spotify.SpotifyPublicCatalogClient.PublicTrack;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MelonChartResolverServiceTest {

    private final MelonChartTrackRepository repository = mock(MelonChartTrackRepository.class);
    private final SpotifyPublicCatalogClient spotify = mock(SpotifyPublicCatalogClient.class);
    private final TidalPlaybackTargetResolverService tidal = mock(TidalPlaybackTargetResolverService.class);
    private final PlatformCredentialService credentials = mock(PlatformCredentialService.class);
    private final MelonChartResolverService service = new MelonChartResolverService(
        repository, spotify, tidal, credentials
    );

    private final MelonChartTrackEntity entity = trackEntity();

    @Test
    void tidalOnlyUserSkipsSpotifyFallbackWhenNoTidalMatch() {
        when(repository.findAll()).thenReturn(List.of(entity));
        when(credentials.findUsableCredential("user-1", "tidal")).thenReturn(Optional.of(stubCredential("tidal")));
        when(credentials.findUsableCredential("user-1", "spotify")).thenReturn(Optional.empty());
        when(tidal.resolve(eq("user-1"), any(TrackQuery.class)))
            .thenThrow(new ApiResourceNotFoundException("no tidal match"));

        Optional<MelonResolveResponse> result = service.resolveByRank(1, "user-1");

        assertThat(result).isPresent();
        assertThat(result.get().resolved()).isFalse();
        assertThat(result.get().sourcePlatform()).isNull();
        assertThat(result.get().spotifyTrackId()).isNull();
        verify(spotify, never()).searchTracks(anyString(), any(Integer.class));
    }

    @Test
    void tidalOnlyUserGetsTidalMatch() {
        when(repository.findAll()).thenReturn(List.of(entity));
        when(credentials.findUsableCredential("user-1", "tidal")).thenReturn(Optional.of(stubCredential("tidal")));
        when(credentials.findUsableCredential("user-1", "spotify")).thenReturn(Optional.empty());
        when(tidal.resolve(eq("user-1"), any(TrackQuery.class))).thenReturn(new TidalPlaybackTarget(
            "tidal-id",
            "tidal:track:tidal-id",
            "Title",
            "Artist",
            "Album",
            "https://image/tidal",
            "https://tidal/song",
            null,
            null,
            210_000,
            "title-artist",
            85
        ));

        Optional<MelonResolveResponse> result = service.resolveByRank(1, "user-1");

        assertThat(result).isPresent();
        assertThat(result.get().resolved()).isTrue();
        assertThat(result.get().sourcePlatform()).isEqualTo("tidal");
        assertThat(result.get().tidalTrackId()).isEqualTo("tidal-id");
    }

    @Test
    void spotifyOnlyUserSkipsTidalAttempt() {
        when(repository.findAll()).thenReturn(List.of(entity));
        when(credentials.findUsableCredential("user-1", "tidal")).thenReturn(Optional.empty());
        when(credentials.findUsableCredential("user-1", "spotify")).thenReturn(Optional.of(stubCredential("spotify")));
        when(spotify.searchTracks(anyString(), any(Integer.class))).thenReturn(List.of(new PublicTrack(
            "sp-id", "Title", "Artist", "Album", "https://image/sp", "https://preview/sp",
            "https://open.spotify.com/track/sp-id", 200_000
        )));

        Optional<MelonResolveResponse> result = service.resolveByRank(1, "user-1");

        assertThat(result).isPresent();
        assertThat(result.get().resolved()).isTrue();
        assertThat(result.get().sourcePlatform()).isEqualTo("spotify");
        assertThat(result.get().spotifyTrackId()).isEqualTo("sp-id");
        verify(tidal, never()).resolve(anyString(), any(TrackQuery.class));
    }

    @Test
    void signedInUserWithNoStreamingCredentialsReturnsUnresolved() {
        when(repository.findAll()).thenReturn(List.of(entity));
        when(credentials.findUsableCredential("user-1", "tidal")).thenReturn(Optional.empty());
        when(credentials.findUsableCredential("user-1", "spotify")).thenReturn(Optional.empty());

        Optional<MelonResolveResponse> result = service.resolveByRank(1, "user-1");

        assertThat(result).isPresent();
        assertThat(result.get().resolved()).isFalse();
        verify(tidal, never()).resolve(anyString(), any(TrackQuery.class));
        verify(spotify, never()).searchTracks(anyString(), any(Integer.class));
    }

    @Test
    void anonymousCallerUsesSpotifyCcWithoutCredentialChecks() {
        when(repository.findAll()).thenReturn(List.of(entity));
        when(spotify.searchTracks(anyString(), any(Integer.class))).thenReturn(List.of(new PublicTrack(
            "sp-id", "Title", "Artist", "Album", "https://image/sp", "https://preview/sp",
            "https://open.spotify.com/track/sp-id", 200_000
        )));

        Optional<MelonResolveResponse> result = service.resolveByRank(1, null);

        assertThat(result).isPresent();
        assertThat(result.get().resolved()).isTrue();
        assertThat(result.get().sourcePlatform()).isEqualTo("spotify");
        verify(credentials, never()).findUsableCredential(anyString(), anyString());
    }

    private MelonChartTrackEntity trackEntity() {
        return new MelonChartTrackEntity(
            1,
            "1",
            "Title",
            "Artist",
            "Album",
            "https://image/melon",
            "https://melon/song/1",
            Instant.parse("2026-05-17T10:00:00Z")
        );
    }

    private PlatformAccountCredential stubCredential(String platformId) {
        return new PlatformAccountCredential(
            "user-1",
            platformId,
            "authorization_code",
            "external-id",
            "Stub account",
            "access-token",
            "refresh-token",
            "Bearer",
            "",
            Instant.parse("2026-12-31T23:59:59Z"),
            Instant.parse("2026-05-10T00:00:00Z"),
            Instant.parse("2026-05-10T00:00:00Z")
        );
    }
}
