package io.myforevermusic.api.modules.platform.infrastructure.spotify;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.myforevermusic.api.modules.auth.application.AuthRegisteredAccount;
import io.myforevermusic.api.modules.platform.application.PlatformAccountCredential;
import io.myforevermusic.api.modules.platform.application.PlatformOAuthProperties;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SpotifyPlatformPlaylistProviderTest {

    @Test
    void shouldListOwnedAndCollaborativeSpotifyPlaylists() {
        SpotifyWebApiClient spotifyWebApiClient = new FakeSpotifyWebApiClient(
            List.of(
                new SpotifyWebApiClient.SpotifyPlaylistSummary(
                    "owned-001",
                    "Night Shift",
                    "Owned by the current Spotify user.",
                    "spotify-user-001",
                    "Forever Listener",
                    false,
                    24
                ),
                new SpotifyWebApiClient.SpotifyPlaylistSummary(
                    "followed-001",
                    "Followed but Locked",
                    "Not accessible for playlist item import.",
                    "other-user-001",
                    "Other User",
                    false,
                    17
                ),
                new SpotifyWebApiClient.SpotifyPlaylistSummary(
                    "collab-001",
                    "Collaborative Rotation",
                    "Collaborative playlist from another Spotify user.",
                    "other-user-002",
                    "Collab Partner",
                    true,
                    11
                )
            ),
            Map.of(),
            Map.of()
        );
        SpotifyPlatformPlaylistProvider provider = new SpotifyPlatformPlaylistProvider(spotifyWebApiClient);

        var playlists = provider.listImportablePlaylists(sampleAccount(), spotifyCredential());

        assertThat(playlists).extracting(playlist -> playlist.externalPlaylistId())
            .containsExactly("owned-001", "collab-001");
        assertThat(playlists).allMatch(playlist -> playlist.tracks().isEmpty());
        assertThat(playlists).extracting(playlist -> playlist.trackCount())
            .containsExactly(24, 11);
    }

    @Test
    void shouldLoadSpotifyTracksAndFallbackWhenAudioFeaturesAreMissing() {
        SpotifyWebApiClient spotifyWebApiClient = new FakeSpotifyWebApiClient(
            List.of(
                new SpotifyWebApiClient.SpotifyPlaylistSummary(
                    "owned-001",
                    "Focus Grid",
                    "Deep focus tracks.",
                    "spotify-user-001",
                    "Forever Listener",
                    false,
                    2
                )
            ),
            Map.of(
                "owned-001",
                List.of(
                    new SpotifyWebApiClient.SpotifyPlaylistTrack(
                        "track-001",
                        "Midnight Receiver",
                        "Neon Bloom",
                        "https://api.spotify.com/v1/tracks/track-001",
                        "spotify:track:track-001",
                        218000
                    ),
                    new SpotifyWebApiClient.SpotifyPlaylistTrack(
                        "track-002",
                        "Quiet Index",
                        "Mono District",
                        "https://api.spotify.com/v1/tracks/track-002",
                        "spotify:track:track-002",
                        221000
                    )
                )
            ),
            Map.of(
                "track-001",
                new SpotifyWebApiClient.SpotifyAudioFeaturesSnapshot(
                    "track-001",
                    "https://api.spotify.com/v1/audio-analysis/track-001",
                    "https://api.spotify.com/v1/tracks/track-001",
                    "spotify:track:track-001",
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
                    Instant.parse("2026-05-03T01:00:00Z")
                )
            )
        );
        SpotifyPlatformPlaylistProvider provider = new SpotifyPlatformPlaylistProvider(spotifyWebApiClient);

        var importedPlaylists = provider.loadPlaylistsForImport(
            sampleAccount(),
            spotifyCredential(),
            List.of("owned-001")
        );

        assertThat(importedPlaylists).hasSize(1);
        assertThat(importedPlaylists.get(0).tracks()).hasSize(2);
        assertThat(importedPlaylists.get(0).tracks().get(0).spotifyAudioFeatures().getAudioFeatureSource())
            .isEqualTo("spotify_api");
        assertThat(importedPlaylists.get(0).tracks().get(0).spotifyAudioFeatures().isComplete()).isTrue();
        assertThat(importedPlaylists.get(0).tracks().get(1).spotifyAudioFeatures().getAudioFeatureSource())
            .isEqualTo("fallback_generated");
        assertThat(importedPlaylists.get(0).tracks().get(1).spotifyAudioFeatures().isComplete()).isTrue();
        assertThat(importedPlaylists.get(0).tracks()).allMatch(track -> track.seed());
    }

    private AuthRegisteredAccount sampleAccount() {
        return new AuthRegisteredAccount(
            "user-001",
            "listener@example.com",
            "listener@example.com",
            "Forever Listener",
            "spotify",
            null,
            null,
            true,
            "platform-onboarding",
            Instant.parse("2026-05-03T00:00:00Z"),
            Instant.parse("2026-05-03T00:00:00Z"),
            Instant.parse("2026-05-03T00:00:00Z")
        );
    }

    private PlatformAccountCredential spotifyCredential() {
        return new PlatformAccountCredential(
            "user-001",
            "spotify",
            "spotify-pkce-draft",
            "spotify-user-001",
            "Forever Listener Spotify account",
            "spotify-access-token",
            "spotify-refresh-token",
            "Bearer",
            "user-read-email, playlist-read-private",
            Instant.parse("2026-05-04T00:00:00Z"),
            Instant.parse("2026-05-03T00:00:00Z"),
            Instant.parse("2026-05-03T00:00:00Z")
        );
    }

    private static final class FakeSpotifyWebApiClient extends SpotifyWebApiClient {

        private final List<SpotifyPlaylistSummary> playlists;
        private final Map<String, List<SpotifyPlaylistTrack>> tracksByPlaylistId;
        private final Map<String, SpotifyAudioFeaturesSnapshot> audioFeaturesByTrackId;

        private FakeSpotifyWebApiClient(
            List<SpotifyPlaylistSummary> playlists,
            Map<String, List<SpotifyPlaylistTrack>> tracksByPlaylistId,
            Map<String, SpotifyAudioFeaturesSnapshot> audioFeaturesByTrackId
        ) {
            super(new PlatformOAuthProperties(), new ObjectMapper());
            this.playlists = playlists;
            this.tracksByPlaylistId = tracksByPlaylistId;
            this.audioFeaturesByTrackId = audioFeaturesByTrackId;
        }

        @Override
        public SpotifyUserProfile getCurrentUserProfile(PlatformAccountCredential credential) {
            return new SpotifyUserProfile("spotify-user-001", "Forever Listener", "listener@example.com");
        }

        @Override
        public List<SpotifyPlaylistSummary> getCurrentUserPlaylists(PlatformAccountCredential credential) {
            return playlists;
        }

        @Override
        public List<SpotifyPlaylistTrack> getPlaylistTracks(
            PlatformAccountCredential credential,
            String externalPlaylistId
        ) {
            return tracksByPlaylistId.getOrDefault(externalPlaylistId, List.of());
        }

        @Override
        public Map<String, SpotifyAudioFeaturesSnapshot> getTrackAudioFeatures(
            PlatformAccountCredential credential,
            List<String> spotifyTrackIds
        ) {
            return audioFeaturesByTrackId.entrySet().stream()
                .filter(entry -> spotifyTrackIds.contains(entry.getKey()))
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue
                ));
        }
    }
}
