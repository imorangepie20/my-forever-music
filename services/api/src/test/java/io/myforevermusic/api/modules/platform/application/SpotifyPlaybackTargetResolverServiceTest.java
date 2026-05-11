package io.myforevermusic.api.modules.platform.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.myforevermusic.api.common.error.ApiResourceNotFoundException;
import io.myforevermusic.api.modules.platform.infrastructure.spotify.SpotifyWebApiClient;
import io.myforevermusic.api.modules.platform.infrastructure.spotify.SpotifyWebApiClient.SpotifySearchResult;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpotifyPlaybackTargetResolverServiceTest {

    @Test
    void shouldResolveBestSpotifyTargetByIsrc() {
        PlatformCredentialService credentialService = mock(PlatformCredentialService.class);
        SpotifyWebApiClient spotifyWebApiClient = mock(SpotifyWebApiClient.class);
        SpotifyPlaybackTargetResolverService service = new SpotifyPlaybackTargetResolverService(
            credentialService,
            spotifyWebApiClient
        );
        PlatformAccountCredential credential = credential();
        when(credentialService.resolveCredential("user-001", "spotify"))
            .thenReturn(PlatformCredentialResolution.ready(credential));
        when(spotifyWebApiClient.searchTracks(credential, "Midnight Signal Neon Bloom", 10))
            .thenReturn(new SpotifySearchResult<>(List.of(
                spotifyTrack("spotify-metadata", "Midnight Signal", "Neon Bloom", "OTHERISRC", 218000),
                spotifyTrack("spotify-isrc", "Midnight Signal - Remaster", "Neon Bloom", "USRC17607839", 223000)
            ), 2));

        var target = service.resolve("user-001", new SpotifyPlaybackTargetResolverService.TrackQuery(
            "Midnight Signal",
            "Neon Bloom",
            "tidal",
            "tidal-track-001",
            "tidal:track:tidal-track-001",
            "tidal-track-001",
            "USRC17607839",
            218000
        ));

        assertThat(target.spotifyTrackId()).isEqualTo("spotify-isrc");
        assertThat(target.matchReason()).isEqualTo("isrc");
        assertThat(target.matchScore()).isEqualTo(100);
        verify(spotifyWebApiClient).searchTracks(credential, "Midnight Signal Neon Bloom", 10);
    }

    @Test
    void shouldResolveMetadataMatchWhenIsrcIsMissing() {
        PlatformCredentialService credentialService = mock(PlatformCredentialService.class);
        SpotifyWebApiClient spotifyWebApiClient = mock(SpotifyWebApiClient.class);
        SpotifyPlaybackTargetResolverService service = new SpotifyPlaybackTargetResolverService(
            credentialService,
            spotifyWebApiClient
        );
        PlatformAccountCredential credential = credential();
        when(credentialService.resolveCredential("user-001", "spotify"))
            .thenReturn(PlatformCredentialResolution.ready(credential));
        when(spotifyWebApiClient.searchTracks(credential, "Quiet Index Mono District", 10))
            .thenReturn(new SpotifySearchResult<>(List.of(spotifyTrack(
                "spotify-metadata",
                "Quiet Index",
                "Mono District",
                null,
                221500
            )), 1));

        var target = service.resolve("user-001", new SpotifyPlaybackTargetResolverService.TrackQuery(
            "Quiet Index",
            "Mono District",
            "tidal",
            "tidal-track-002",
            "tidal:track:tidal-track-002",
            "tidal-track-002",
            null,
            221000
        ));

        assertThat(target.spotifyTrackId()).isEqualTo("spotify-metadata");
        assertThat(target.matchReason()).isEqualTo("metadata");
        assertThat(target.matchScore()).isEqualTo(100);
    }

    @Test
    void shouldFailWhenNoSpotifyCandidateMatches() {
        PlatformCredentialService credentialService = mock(PlatformCredentialService.class);
        SpotifyWebApiClient spotifyWebApiClient = mock(SpotifyWebApiClient.class);
        SpotifyPlaybackTargetResolverService service = new SpotifyPlaybackTargetResolverService(
            credentialService,
            spotifyWebApiClient
        );
        PlatformAccountCredential credential = credential();
        when(credentialService.resolveCredential("user-001", "spotify"))
            .thenReturn(PlatformCredentialResolution.ready(credential));
        when(spotifyWebApiClient.searchTracks(credential, "Unknown Track Unknown Artist", 10))
            .thenReturn(new SpotifySearchResult<>(List.of(
                spotifyTrack("spotify-other", "Other Track", "Other Artist", null, 180000)
            ), 1));

        assertThatThrownBy(() -> service.resolve(
            "user-001",
            new SpotifyPlaybackTargetResolverService.TrackQuery(
                "Unknown Track",
                "Unknown Artist",
                "tidal",
                "tidal-track-404",
                "tidal:track:tidal-track-404",
                "tidal-track-404",
                null,
                240000
            )
        )).isInstanceOf(ApiResourceNotFoundException.class)
            .hasMessageContaining("No playable Spotify match");
    }

    private PlatformAccountCredential credential() {
        return new PlatformAccountCredential(
            "user-001",
            "spotify",
            "spotify-oauth",
            "spotify-user-001",
            "Forever Listener Spotify",
            "access-token",
            "refresh-token",
            "Bearer",
            "streaming user-read-playback-state",
            Instant.now().plusSeconds(3600),
            Instant.parse("2026-05-03T00:00:00Z"),
            Instant.parse("2026-05-03T00:00:00Z")
        );
    }

    private SpotifyWebApiClient.SpotifyPlaylistTrack spotifyTrack(
        String trackId,
        String title,
        String artistName,
        String isrc,
        int durationMs
    ) {
        return new SpotifyWebApiClient.SpotifyPlaylistTrack(
            trackId,
            title,
            artistName,
            "Album",
            null,
            "https://api.spotify.com/v1/tracks/" + trackId,
            "https://open.spotify.com/track/" + trackId,
            "spotify:track:" + trackId,
            null,
            isrc,
            durationMs
        );
    }
}
