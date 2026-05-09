package io.myforevermusic.api.modules.platform.infrastructure.tidal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.myforevermusic.api.modules.auth.application.AuthRegisteredAccount;
import io.myforevermusic.api.modules.platform.application.PlatformAccountCredential;
import io.myforevermusic.api.modules.platform.application.PlatformOAuthProperties;
import io.myforevermusic.api.modules.platform.infrastructure.reccobeats.ReccoBeatsAudioFeaturesClient;
import io.myforevermusic.api.modules.platform.infrastructure.reccobeats.ReccoBeatsAudioFeaturesClient.ReccoBeatsAudioFeaturesSnapshot;

import io.myforevermusic.api.modules.platform.infrastructure.reccobeats.ReccoBeatsProperties;
import java.net.http.HttpClient;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class TidalPlatformPlaylistProviderTest {

    @Test
    void shouldLoadTidalTracksWithReccoBeatsIsrcMatches() {
        TidalPlatformPlaylistProvider provider = new TidalPlatformPlaylistProvider(
            new FakeTidalWebApiClient(
                List.of(new TidalWebApiClient.TidalPlaylistSummary(
                    "playlist-001",
                    "Noir Rotation",
                    "Imported from TIDAL.",
                    2,
                    null,
                    null,
                    "https://tidal.com/browse/playlist/playlist-001",
                    "playlist-uuid-001"
                )),
                Map.of(
                    "playlist-001",
                    List.of(
                        new TidalWebApiClient.TidalPlaylistTrack(
                            "tidal-track-001",
                            "Midnight Receiver",
                            "Neon Bloom",
                            "Signal Bloom",
                            null,
                            "https://tidal.com/browse/track/tidal-track-001",
                            "tidal:track:tidal-track-001",
                            null,
                            "USRC17607839",
                            218000
                        ),
                        new TidalWebApiClient.TidalPlaylistTrack(
                            "tidal-track-002",
                            "Quiet Index",
                            "Mono District",
                            "Focus Grid",
                            null,
                            "https://tidal.com/browse/track/tidal-track-002",
                            "tidal:track:tidal-track-002",
                            null,
                            "USRC17607840",
                            221000
                        )
                    )
                )
            ),
            new FakeReccoBeatsAudioFeaturesClient(Map.of(
                "tidal-track-001", new ReccoBeatsAudioFeaturesSnapshot(
                    "spotify-track-001",
                    "recco-uuid-001",
                    "https://open.spotify.com/track/spotify-track-001",
                    "USRC17607839",
                    0.19,
                    0.74,
                    0.78,
                    0.02,
                    1,
                    0.11,
                    -7.8,
                    1,
                    0.05,
                    116.2,
                    0.67,
                    Instant.parse("2026-05-03T01:00:00Z")
                ),
                "tidal-track-002", new ReccoBeatsAudioFeaturesSnapshot(
                    "spotify-track-002",
                    "recco-uuid-002",
                    "https://open.spotify.com/track/spotify-track-002",
                    "USRC17607840",
                    0.16,
                    0.71,
                    0.69,
                    0.01,
                    2,
                    0.12,
                    -8.1,
                    1,
                    0.04,
                    112.4,
                    0.61,
                    Instant.parse("2026-05-03T01:00:00Z")
                )
            ))
        );

        var importedPlaylists = provider.loadPlaylistsForImport(
            sampleAccount(),
            tidalCredential(),
            List.of("playlist-001")
        );

        assertThat(importedPlaylists).hasSize(1);
        assertThat(importedPlaylists.get(0).tracks()).hasSize(2);
        assertThat(importedPlaylists.get(0).tracks()).allMatch(track -> track.seed());
        assertThat(importedPlaylists.get(0).tracks().get(0).audioFeatures().getAudioFeatureSource())
            .isEqualTo("reccobeats_isrc_match");
        assertThat(importedPlaylists.get(0).tracks().get(0).audioFeatures().isComplete()).isTrue();
        assertThat(importedPlaylists.get(0).tracks().get(0).audioFeatures().getAudioFeatureTrackId())
            .isEqualTo("spotify-track-001");
    }

    @Test
    void shouldStorePlaceholderWhenTidalIsrcLookupHasNoMatch() {
        TidalPlatformPlaylistProvider provider = new TidalPlatformPlaylistProvider(
            new FakeTidalWebApiClient(
                List.of(new TidalWebApiClient.TidalPlaylistSummary(
                    "playlist-001",
                    "Noir Rotation",
                    "Imported from TIDAL.",
                    1,
                    null,
                    null,
                    "https://tidal.com/browse/playlist/playlist-001",
                    "playlist-uuid-001"
                )),
                Map.of(
                    "playlist-001",
                    List.of(new TidalWebApiClient.TidalPlaylistTrack(
                        "tidal-track-001",
                        "Midnight Receiver",
                        "Neon Bloom",
                        "Signal Bloom",
                        null,
                        "https://tidal.com/browse/track/tidal-track-001",
                        "tidal:track:tidal-track-001",
                        null,
                        "USRC17607839",
                        218000
                    ))
                )
            ),
            new FakeReccoBeatsAudioFeaturesClient(Map.of())
        );

        var importedPlaylists = provider.loadPlaylistsForImport(
            sampleAccount(),
            tidalCredential(),
            List.of("playlist-001")
        );

        assertThat(importedPlaylists).hasSize(1);
        assertThat(importedPlaylists.get(0).tracks()).hasSize(1);
        assertThat(importedPlaylists.get(0).tracks().get(0).audioFeatures().getAudioFeatureSource())
            .isEqualTo("unavailable");
        assertThat(importedPlaylists.get(0).tracks().get(0).audioFeatures().isComplete()).isFalse();
    }

    private AuthRegisteredAccount sampleAccount() {
        return new AuthRegisteredAccount(
            "user-001",
            "listener@example.com",
            "listener@example.com",
            "Forever Listener",
            "tidal",
            null,
            null,
            true,
            "platform-onboarding",
            Instant.parse("2026-05-03T00:00:00Z"),
            Instant.parse("2026-05-03T00:00:00Z"),
            Instant.parse("2026-05-03T00:00:00Z")
        );
    }

    private PlatformAccountCredential tidalCredential() {
        return new PlatformAccountCredential(
            "user-001",
            "tidal",
            "tidal-pkce-draft",
            "tidal-user-001",
            "Forever Listener TIDAL account",
            "tidal-access-token",
            "tidal-refresh-token",
            "Bearer",
            "playlist-read-private",
            Instant.parse("2026-05-04T00:00:00Z"),
            Instant.parse("2026-05-03T00:00:00Z"),
            Instant.parse("2026-05-03T00:00:00Z")
        );
    }

    private static final class FakeTidalWebApiClient extends TidalWebApiClient {

        private final List<TidalPlaylistSummary> playlists;
        private final Map<String, List<TidalPlaylistTrack>> tracksByPlaylistId;

        private FakeTidalWebApiClient(
            List<TidalPlaylistSummary> playlists,
            Map<String, List<TidalPlaylistTrack>> tracksByPlaylistId
        ) {
            super(new PlatformOAuthProperties(), new ObjectMapper(), HttpClient.newHttpClient(), "https://openapi.tidal.com/v2");
            this.playlists = playlists;
            this.tracksByPlaylistId = tracksByPlaylistId;
        }

        @Override
        public TidalUserProfile getCurrentUserProfile(PlatformAccountCredential credential) {
            return new TidalUserProfile("tidal-user-001", "tidal-user-001", "Forever", "Listener", "listener@example.com");
        }

        @Override
        public List<TidalPlaylistSummary> getUserPlaylists(PlatformAccountCredential credential) {
            return playlists;
        }

        @Override
        public List<TidalPlaylistTrack> getPlaylistTracks(PlatformAccountCredential credential, String playlistId) {
            return tracksByPlaylistId.getOrDefault(playlistId, List.of());
        }
    }

    private static final class FakeReccoBeatsAudioFeaturesClient extends ReccoBeatsAudioFeaturesClient {

        private final Map<String, ReccoBeatsAudioFeaturesSnapshot> audioFeaturesByExternalTrackId;

        private FakeReccoBeatsAudioFeaturesClient(
            Map<String, ReccoBeatsAudioFeaturesSnapshot> audioFeaturesByExternalTrackId
        ) {
            super(new ReccoBeatsProperties(), new ObjectMapper());
            this.audioFeaturesByExternalTrackId = audioFeaturesByExternalTrackId;
        }

        @Override
        public Map<String, ReccoBeatsAudioFeaturesSnapshot> getAudioFeaturesForExternalTracksByIsrc(
            List<ReccoBeatsTrackLookupRequest> trackRequests
        ) {
            return trackRequests.stream()
                .map(ReccoBeatsTrackLookupRequest::externalTrackId)
                .filter(audioFeaturesByExternalTrackId::containsKey)
                .distinct()
                .collect(Collectors.toMap(
                    externalTrackId -> externalTrackId,
                    audioFeaturesByExternalTrackId::get
                ));
        }
    }
}
