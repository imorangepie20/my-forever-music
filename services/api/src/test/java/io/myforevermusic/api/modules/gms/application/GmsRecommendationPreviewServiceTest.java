package io.myforevermusic.api.modules.gms.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.myforevermusic.api.modules.auth.application.AuthRegistrationService;
import io.myforevermusic.api.modules.auth.infrastructure.local.InMemoryAuthAccountStore;
import io.myforevermusic.api.modules.auth.presentation.AuthRegistrationRequest;
import io.myforevermusic.api.modules.gms.infrastructure.ai.AiRecommendationPreviewClient;
import io.myforevermusic.api.modules.gms.presentation.GmsRecommendationPreviewRequest;
import io.myforevermusic.api.modules.gms.presentation.GmsRecommendationPreviewResponse;
import io.myforevermusic.api.modules.platform.application.LastFmProperties;
import io.myforevermusic.api.modules.platform.application.LastFmScrobbleStore;
import io.myforevermusic.api.modules.platform.infrastructure.lastfm.LastFmWebApiClient;
import io.myforevermusic.api.modules.platform.infrastructure.local.InMemoryLastFmScrobbleStore;
import io.myforevermusic.api.modules.pms.infrastructure.local.InMemoryPmsUserLibraryStore;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class GmsRecommendationPreviewServiceTest {

    @Test
    void shouldBlendSavedLastFmArtistsIntoGmsRequest() {
        InMemoryAuthAccountStore authAccountStore = new InMemoryAuthAccountStore();
        AuthRegistrationService authRegistrationService = new AuthRegistrationService(
            authAccountStore,
            new BCryptPasswordEncoder()
        );
        String userId = authRegistrationService.register(new AuthRegistrationRequest(
            "Forever Listener",
            "gms-lastfm@example.com",
            "music2026",
            "spotify",
            false,
            true,
            true
        )).user().userId();
        authAccountStore.saveLastFmProfile(userId, "mibeen", Instant.parse("2026-05-04T00:00:00Z"));

        CapturingAiRecommendationPreviewClient aiClient = new CapturingAiRecommendationPreviewClient();
        GmsRecommendationPreviewService service = new GmsRecommendationPreviewService(
            aiClient,
            authAccountStore,
            new InMemoryLastFmScrobbleStore(),
            new InMemoryPmsUserLibraryStore(),
            Optional.of(new FakeLastFmWebApiClient())
        );

        GmsRecommendationPreviewResponse response = service.previewRecommendations(
            new GmsRecommendationPreviewRequest(
                "preview-001",
                userId,
                "playlist-001",
                "gms",
                "upbeat",
                4,
                3,
                5,
                List.of("track-alpha"),
                List.of("Artist One"),
                List.of("synth-pop"),
                true
            )
        );

        assertThat(aiClient.capturedRequest.seedArtistNames()).contains("Artist One", "The Midnight", "M83");
        assertThat(response.warnings()).anyMatch(warning -> warning.contains("Saved Last.fm profile 'mibeen'"));
    }

    @Test
    void shouldPreferStoredLastFmScrobbleSnapshotForGmsRequest() {
        InMemoryAuthAccountStore authAccountStore = new InMemoryAuthAccountStore();
        InMemoryLastFmScrobbleStore scrobbleStore = new InMemoryLastFmScrobbleStore();
        AuthRegistrationService authRegistrationService = new AuthRegistrationService(
            authAccountStore,
            new BCryptPasswordEncoder()
        );
        String userId = authRegistrationService.register(new AuthRegistrationRequest(
            "Forever Listener",
            "gms-stored-lastfm@example.com",
            "music2026",
            "spotify",
            false,
            true,
            true
        )).user().userId();
        authAccountStore.saveLastFmProfile(userId, "mibeen", Instant.parse("2026-05-04T00:00:00Z"));
        scrobbleStore.saveScrobbles(
            userId,
            "mibeen",
            Instant.parse("2026-05-04T08:00:00Z"),
            List.of(
                new LastFmScrobbleStore.StoredScrobble(
                    userId,
                    "mibeen",
                    "Genesis",
                    "Grimes",
                    "Visions",
                    null,
                    null,
                    Instant.parse("2026-05-04T06:00:00Z"),
                    true,
                    Instant.parse("2026-05-04T08:00:00Z")
                ),
                new LastFmScrobbleStore.StoredScrobble(
                    userId,
                    "mibeen",
                    "Oblivion",
                    "Grimes",
                    "Visions",
                    null,
                    null,
                    Instant.parse("2026-05-04T05:00:00Z"),
                    false,
                    Instant.parse("2026-05-04T08:00:00Z")
                ),
                new LastFmScrobbleStore.StoredScrobble(
                    userId,
                    "mibeen",
                    "Odessa",
                    "Caribou",
                    "Swim",
                    null,
                    null,
                    Instant.parse("2026-05-03T23:00:00Z"),
                    false,
                    Instant.parse("2026-05-04T08:00:00Z")
                )
            )
        );

        CapturingAiRecommendationPreviewClient aiClient = new CapturingAiRecommendationPreviewClient();
        GmsRecommendationPreviewService service = new GmsRecommendationPreviewService(
            aiClient,
            authAccountStore,
            scrobbleStore,
            new InMemoryPmsUserLibraryStore(),
            Optional.of(new FakeLastFmWebApiClient())
        );

        GmsRecommendationPreviewResponse response = service.previewRecommendations(
            new GmsRecommendationPreviewRequest(
                "preview-002",
                userId,
                "playlist-001",
                "gms",
                "upbeat",
                4,
                3,
                5,
                List.of("track-alpha"),
                List.of("Artist One"),
                List.of("synth-pop"),
                true
            )
        );

        assertThat(aiClient.capturedRequest.seedArtistNames()).contains("Artist One", "Grimes", "Caribou");
        assertThat(aiClient.capturedRequest.seedArtistNames()).doesNotContain("The Midnight");
        assertThat(response.warnings()).anyMatch(warning -> warning.contains("Stored Last.fm scrobble snapshot"));
    }

    private static final class CapturingAiRecommendationPreviewClient extends AiRecommendationPreviewClient {

        private GmsRecommendationPreviewRequest capturedRequest;

        private CapturingAiRecommendationPreviewClient() {
            super(new io.myforevermusic.api.modules.gms.infrastructure.ai.AiServiceProperties("http://localhost:8000", "/v1/recommendations/preview", "/v1/ems/overview"), new ObjectMapper());
        }

        @Override
        public GmsRecommendationPreviewResponse requestPreview(GmsRecommendationPreviewRequest request) {
            this.capturedRequest = request;
            return new GmsRecommendationPreviewResponse(
                "preview-001",
                Instant.parse("2026-05-04T01:00:00Z"),
                "ai",
                "ok",
                new GmsRecommendationPreviewResponse.RecommendationContext(
                    "gms-hybrid-blend",
                    "rule-based-preview-v1",
                    "gms",
                    "upbeat",
                    4,
                    List.of("track-alpha", "artist-one", "the-midnight", "m83")
                ),
                new GmsRecommendationPreviewResponse.RecommendationInputSummary(
                    request.userId(),
                    request.playlistId(),
                    request.seedTrackIds().size(),
                    request.seedArtistNames().size(),
                    request.seedGenres().size(),
                    request.familiarityBias(),
                    request.limit()
                ),
                List.of(),
                List.of()
            );
        }
    }

    private static final class FakeLastFmWebApiClient extends LastFmWebApiClient {

        private FakeLastFmWebApiClient() {
            super(new LastFmProperties(), new ObjectMapper());
        }

        @Override
        public List<LastFmTopArtist> getTopArtists(String username, String period, int limit) {
            return List.of(
                new LastFmTopArtist("The Midnight", 1, 88L, "https://www.last.fm/music/The+Midnight", null),
                new LastFmTopArtist("M83", 2, 56L, "https://www.last.fm/music/M83", null)
            );
        }
    }
}
