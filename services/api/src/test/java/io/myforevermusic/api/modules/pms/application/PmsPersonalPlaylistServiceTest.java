package io.myforevermusic.api.modules.pms.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.myforevermusic.api.modules.pms.infrastructure.local.InMemoryPmsPersonalPlaylistStore;
import io.myforevermusic.api.modules.pms.infrastructure.local.InMemoryPmsUserLibraryStore;
import io.myforevermusic.api.modules.pms.infrastructure.persistence.PmsTrackAudioFeatures;
import io.myforevermusic.api.modules.pms.presentation.PmsPersonalPlaylistCommandResponse;
import io.myforevermusic.api.modules.pms.presentation.PmsPersonalPlaylistCreateRequest;
import io.myforevermusic.api.modules.pms.presentation.PmsPersonalPlaylistTrackSaveRequest;
import io.myforevermusic.api.modules.recommendation.application.UserMusicEventService;
import io.myforevermusic.api.modules.recommendation.infrastructure.local.InMemoryUserMusicEventStore;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PmsPersonalPlaylistServiceTest {

    @Test
    void shouldCreatePersonalPlaylist() {
        PmsPersonalPlaylistService service = new PmsPersonalPlaylistService(
            new InMemoryPmsPersonalPlaylistStore(),
            new InMemoryPmsUserLibraryStore(),
            new UserMusicEventService(new InMemoryUserMusicEventStore())
        );

        PmsPersonalPlaylistCommandResponse response = service.createPlaylist(
            new PmsPersonalPlaylistCreateRequest(
                "user-001",
                "Late Night Saves",
                "Tracks worth keeping."
            )
        );

        assertThat(response.status()).isEqualTo("created");
        assertThat(response.playlist().playlistId()).startsWith("personal-late-night-saves-");
        assertThat(response.playlist().trackCount()).isZero();
    }

    @Test
    void shouldSaveLibraryTrackIntoDefaultGmsPlaylist() {
        InMemoryPmsUserLibraryStore userLibraryStore = new InMemoryPmsUserLibraryStore();
        InMemoryUserMusicEventStore eventStore = new InMemoryUserMusicEventStore();
        userLibraryStore.savePlaylists("user-001", List.of(sampleLibraryPlaylist()));
        PmsPersonalPlaylistService service = new PmsPersonalPlaylistService(
            new InMemoryPmsPersonalPlaylistStore(),
            userLibraryStore,
            new UserMusicEventService(eventStore)
        );

        PmsPersonalPlaylistCommandResponse response = service.saveTrack(
            new PmsPersonalPlaylistTrackSaveRequest(
                "user-001",
                null,
                "Saved GMS Recommendations",
                "track-001",
                "gms-preview"
            )
        );
        PmsPersonalPlaylistCommandResponse duplicateResponse = service.saveTrack(
            new PmsPersonalPlaylistTrackSaveRequest(
                "user-001",
                response.playlist().playlistId(),
                null,
                "track-001",
                "gms-preview"
            )
        );

        assertThat(response.status()).isEqualTo("saved");
        assertThat(response.playlist().playlistId()).isEqualTo("personal-saved-gms-recommendations");
        assertThat(response.playlist().trackCount()).isEqualTo(1);
        assertThat(response.playlist().tracks().getFirst().spotifyTrackId()).isEqualTo("spotify-track-001");
        assertThat(response.playlist().tracks().getFirst().audioFeatureTrackId()).isEqualTo("spotify-track-001");
        assertThat(duplicateResponse.playlist().trackCount()).isEqualTo(1);
        assertThat(eventStore.findRecentByUserId("user-001", 10))
            .extracting("eventType")
            .containsOnly("added_to_playlist")
            .hasSize(2);
        assertThat(eventStore.findRecentByUserId("user-001", 1).getFirst().playlistId())
            .isEqualTo("personal-saved-gms-recommendations");
    }

    @Test
    void shouldRejectTrackOutsideSyncedPmsLibrary() {
        PmsPersonalPlaylistService service = new PmsPersonalPlaylistService(
            new InMemoryPmsPersonalPlaylistStore(),
            new InMemoryPmsUserLibraryStore(),
            new UserMusicEventService(new InMemoryUserMusicEventStore())
        );

        assertThatThrownBy(() -> service.saveTrack(
            new PmsPersonalPlaylistTrackSaveRequest(
                "user-001",
                null,
                null,
                "track-missing",
                "gms-preview"
            )
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("PMS user library");
    }

    private PmsUserLibraryStore.LibraryPlaylistState sampleLibraryPlaylist() {
        return new PmsUserLibraryStore.LibraryPlaylistState(
            "user-001",
            "playlist-001",
            "spotify-playlist-001",
            "Night Drive Archive",
            "spotify",
            "Forever Listener",
            "Synced from imported playlists.",
            null,
            "https://open.spotify.com/playlist/spotify-playlist-001",
            "spotify:playlist:spotify-playlist-001",
            Instant.parse("2026-05-04T00:00:00Z"),
            List.of(
                new PmsUserLibraryStore.LibraryTrackState(
                    "track-001",
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
        );
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
