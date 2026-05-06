package io.myforevermusic.api.modules.pms.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.myforevermusic.api.modules.pms.infrastructure.local.InMemoryPmsUserLibraryStore;
import io.myforevermusic.api.modules.pms.infrastructure.persistence.PmsTrackAudioFeatures;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PmsUserLibrarySyncServiceTest {

    @Test
    void shouldSyncImportedPlaylistsIntoFormalUserLibrary() {
        InMemoryPmsUserLibraryStore userLibraryStore = new InMemoryPmsUserLibraryStore();
        PmsUserLibrarySyncService service = new PmsUserLibrarySyncService(userLibraryStore);

        Instant syncedAt = Instant.parse("2026-05-04T00:00:00Z");
        List<PmsPlaylistImportStore.ImportedPlaylistState> importedPlaylists = List.of(
            new PmsPlaylistImportStore.ImportedPlaylistState(
                "user-001",
                "pms-spotify-playlist-001",
                "spotify-playlist-001",
                "Night Drive Archive",
                "spotify",
                "Forever Listener",
                "Imported from a local-first PMS sync.",
                null,
                "https://open.spotify.com/playlist/spotify-playlist-001",
                "spotify:playlist:spotify-playlist-001",
                syncedAt,
                List.of(
                    new PmsPlaylistImportStore.ImportedTrackState(
                        "pms-track-spotify-track-001",
                        "spotify-track-001",
                        "Midnight Receiver",
                        "Neon Bloom",
                        "spotify",
                        "synth-pop",
                        "Signal Bloom",
                        null,
                        "https://open.spotify.com/track/spotify-track-001",
                        "spotify:track:spotify-track-001",
                        null,
                        1,
                        true,
                        sampleFeatures("spotify-track-001")
                    )
                )
            )
        );

        List<PmsUserLibraryStore.LibraryPlaylistState> savedPlaylists = service.syncImportedPlaylists(
            "user-001",
            importedPlaylists,
            syncedAt
        );

        assertThat(savedPlaylists).hasSize(1);
        assertThat(savedPlaylists.getFirst().playlistId()).isEqualTo("pms-spotify-playlist-001");
        assertThat(savedPlaylists.getFirst().trackCount()).isEqualTo(1);
        assertThat(savedPlaylists.getFirst().tracks().getFirst().seed()).isTrue();
        assertThat(savedPlaylists.getFirst().tracks().getFirst().audioFeatures().isComplete()).isTrue();
        assertThat(userLibraryStore.findPlaylists("user-001")).hasSize(1);
    }

    private PmsTrackAudioFeatures sampleFeatures(String spotifyTrackId) {
        return new PmsTrackAudioFeatures(
            spotifyTrackId,
            "spotify_api",
            true,
            "https://api.spotify.com/v1/audio-analysis/%s".formatted(spotifyTrackId),
            "https://api.spotify.com/v1/tracks/%s".formatted(spotifyTrackId),
            "spotify:track:%s".formatted(spotifyTrackId),
            "audio_features",
            218000,
            1,
            1,
            4,
            0.19,
            0.74,
            0.78,
            0.02,
            0.11,
            -7.8,
            0.05,
            116.2,
            0.67,
            Instant.parse("2026-05-04T00:00:00Z")
        );
    }
}
