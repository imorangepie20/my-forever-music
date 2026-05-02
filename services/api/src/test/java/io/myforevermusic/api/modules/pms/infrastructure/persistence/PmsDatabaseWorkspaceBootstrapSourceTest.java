package io.myforevermusic.api.modules.pms.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.myforevermusic.api.modules.pms.presentation.PmsWorkspaceBootstrapResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PmsDatabaseWorkspaceBootstrapSourceTest {

    @Mock
    private PmsCatalogPlaylistRepository playlistRepository;

    @Mock
    private PmsCatalogPlaylistTrackRepository playlistTrackRepository;

    @Test
    void shouldProjectDatabaseCatalogToBootstrapResponse() {
        PmsCatalogPlaylistEntity playlist = new PmsCatalogPlaylistEntity(
            "playlist-001",
            "user-001",
            "Forever Midnight Drive",
            "spotify",
            4,
            "system",
            "High replay consistency and strong synth-pop overlap.",
            1
        );

        PmsCatalogTrackEntity trackAlpha = new PmsCatalogTrackEntity(
            "track-alpha",
            "Track Alpha",
            "Artist One",
            "spotify",
            "synth-pop"
        );
        PmsCatalogTrackEntity trackBeta = new PmsCatalogTrackEntity(
            "track-beta",
            "Track Beta",
            "Artist Two",
            "apple-music",
            "dream-pop"
        );
        PmsCatalogTrackEntity trackGamma = new PmsCatalogTrackEntity(
            "track-gamma",
            "Track Gamma",
            "Artist One",
            "spotify",
            "synth-pop"
        );
        PmsCatalogTrackEntity trackDelta = new PmsCatalogTrackEntity(
            "track-delta",
            "Track Delta",
            "Artist Three",
            "tidal",
            "indietronica"
        );

        when(playlistRepository.findAllByOrderByDisplayOrderAscIdAsc())
            .thenReturn(List.of(playlist));
        when(playlistTrackRepository.findByPlaylist_IdOrderBySortOrderAscIdAsc("playlist-001"))
            .thenReturn(List.of(
                new PmsCatalogPlaylistTrackEntity(1L, playlist, trackAlpha, 1, true),
                new PmsCatalogPlaylistTrackEntity(2L, playlist, trackBeta, 2, true),
                new PmsCatalogPlaylistTrackEntity(3L, playlist, trackGamma, 3, false),
                new PmsCatalogPlaylistTrackEntity(4L, playlist, trackDelta, 4, false)
            ));

        PmsDatabaseWorkspaceBootstrapSource source = new PmsDatabaseWorkspaceBootstrapSource(
            playlistRepository,
            playlistTrackRepository
        );

        Optional<PmsWorkspaceBootstrapResponse> result = source.load();

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().workspaceDefaults().playlistId()).isEqualTo("playlist-001");
        assertThat(result.orElseThrow().workspaceDefaults().seedTrackIds())
            .containsExactly("track-alpha", "track-beta");
        assertThat(result.orElseThrow().workspaceDefaults().seedArtistNames())
            .containsExactly("Artist One", "Artist Two");
        assertThat(result.orElseThrow().workspaceDefaults().seedGenres())
            .containsExactly("synth-pop", "dream-pop");
        assertThat(result.orElseThrow().playlists()).hasSize(1);
        assertThat(result.orElseThrow().suggestedTracks()).hasSize(4);
        assertThat(result.orElseThrow().suggestedArtists().getFirst().artistName()).isEqualTo("Artist One");
        assertThat(result.orElseThrow().suggestedGenres().getFirst().genre()).isEqualTo("synth-pop");
    }
}
