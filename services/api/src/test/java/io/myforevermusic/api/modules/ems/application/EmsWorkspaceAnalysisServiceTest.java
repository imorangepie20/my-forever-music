package io.myforevermusic.api.modules.ems.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.myforevermusic.api.modules.auth.application.AuthRegistrationService;
import io.myforevermusic.api.modules.auth.infrastructure.local.InMemoryAuthAccountStore;
import io.myforevermusic.api.modules.ems.presentation.EmsWorkspaceAnalysisRequest;
import io.myforevermusic.api.modules.ems.presentation.EmsWorkspaceAnalysisResponse;
import io.myforevermusic.api.modules.platform.application.LastFmProperties;
import io.myforevermusic.api.modules.platform.application.LastFmScrobbleStore;
import io.myforevermusic.api.modules.platform.infrastructure.lastfm.LastFmWebApiClient;
import io.myforevermusic.api.modules.platform.infrastructure.local.InMemoryLastFmScrobbleStore;
import io.myforevermusic.api.modules.pms.infrastructure.persistence.PmsCatalogTrackEntity;
import io.myforevermusic.api.modules.pms.infrastructure.persistence.PmsCatalogTrackRepository;
import io.myforevermusic.api.modules.auth.presentation.AuthRegistrationRequest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class EmsWorkspaceAnalysisServiceTest {

    @Test
    void shouldRecommendCalmerProfileFromTextSeedsOnly() {
        EmsWorkspaceAnalysisService service = new EmsWorkspaceAnalysisService(
            Optional.empty(),
            Optional.empty(),
            new InMemoryAuthAccountStore(),
            new InMemoryLastFmScrobbleStore(),
            Optional.empty()
        );

        EmsWorkspaceAnalysisResponse response = service.analyzeWorkspace(
            new EmsWorkspaceAnalysisRequest(
                "user-001",
                "playlist-002",
                List.of(),
                List.of("Artist Four"),
                List.of("ambient-pop", "dream-pop")
            )
        );

        assertThat(response.workspaceRecommendation().mood()).isEqualTo("calm");
        assertThat(response.workspaceRecommendation().energyLevel()).isLessThanOrEqualTo(3);
        assertThat(response.topSignals()).isNotEmpty();
    }

    @Test
    void shouldUseCatalogTracksWhenAvailable() {
        PmsCatalogTrackRepository trackRepository = mock(PmsCatalogTrackRepository.class);
        when(trackRepository.findAllById(anyIterable())).thenReturn(List.of(
            new PmsCatalogTrackEntity("track-alpha", "Track Alpha", "Artist One", "spotify", "synth-pop"),
            new PmsCatalogTrackEntity("track-gamma", "Track Gamma", "Artist One", "spotify", "indietronica")
        ));

        EmsWorkspaceAnalysisService service = new EmsWorkspaceAnalysisService(
            Optional.of(trackRepository),
            Optional.empty(),
            new InMemoryAuthAccountStore(),
            new InMemoryLastFmScrobbleStore(),
            Optional.empty()
        );

        EmsWorkspaceAnalysisResponse response = service.analyzeWorkspace(
            new EmsWorkspaceAnalysisRequest(
                "user-001",
                "playlist-001",
                List.of("track-alpha", "track-gamma"),
                List.of(),
                List.of("synth-pop")
            )
        );

        assertThat(response.context().matchedCatalogTrackCount()).isEqualTo(2);
        assertThat(response.workspaceRecommendation().familiarityBias()).isGreaterThanOrEqualTo(4);
        assertThat(response.notes()).anyMatch(note -> note.contains("Matched catalog tracks"));
    }

    @Test
    void shouldBlendSavedLastFmTopArtistsIntoArtistSignals() {
        InMemoryAuthAccountStore authAccountStore = new InMemoryAuthAccountStore();
        AuthRegistrationService authRegistrationService = new AuthRegistrationService(
            authAccountStore,
            new BCryptPasswordEncoder()
        );
        String userId = authRegistrationService.register(new AuthRegistrationRequest(
            "Forever Listener",
            "lastfm-ems@example.com",
            "music2026",
            "spotify",
            false,
            true,
            true
        )).user().userId();
        authAccountStore.saveLastFmProfile(userId, "mibeen", java.time.Instant.parse("2026-05-04T00:00:00Z"));

        EmsWorkspaceAnalysisService service = new EmsWorkspaceAnalysisService(
            Optional.empty(),
            Optional.empty(),
            authAccountStore,
            new InMemoryLastFmScrobbleStore(),
            Optional.of(new FakeLastFmWebApiClient())
        );

        EmsWorkspaceAnalysisResponse response = service.analyzeWorkspace(
            new EmsWorkspaceAnalysisRequest(
                userId,
                null,
                List.of(),
                List.of(),
                List.of("dream-pop")
            )
        );

        assertThat(response.topSignals()).anyMatch(signal ->
            signal.type().equals("artist") && signal.label().equals("The Midnight")
        );
        assertThat(response.notes()).anyMatch(note -> note.contains("Linked Last.fm profile 'mibeen'"));
    }

    @Test
    void shouldPreferStoredLastFmScrobblesBeforeLiveTopArtists() {
        InMemoryAuthAccountStore authAccountStore = new InMemoryAuthAccountStore();
        InMemoryLastFmScrobbleStore scrobbleStore = new InMemoryLastFmScrobbleStore();
        AuthRegistrationService authRegistrationService = new AuthRegistrationService(
            authAccountStore,
            new BCryptPasswordEncoder()
        );
        String userId = authRegistrationService.register(new AuthRegistrationRequest(
            "Forever Listener",
            "stored-lastfm-ems@example.com",
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
                    "Days of Thunder",
                    "The Midnight",
                    "Days of Thunder",
                    null,
                    null,
                    Instant.parse("2026-05-04T07:00:00Z"),
                    true,
                    Instant.parse("2026-05-04T08:00:00Z")
                ),
                new LastFmScrobbleStore.StoredScrobble(
                    userId,
                    "mibeen",
                    "Sunset",
                    "The Midnight",
                    "Endless Summer",
                    null,
                    null,
                    Instant.parse("2026-05-03T21:00:00Z"),
                    false,
                    Instant.parse("2026-05-04T08:00:00Z")
                ),
                new LastFmScrobbleStore.StoredScrobble(
                    userId,
                    "mibeen",
                    "Awake",
                    "Tycho",
                    "Awake",
                    null,
                    null,
                    Instant.parse("2026-05-03T18:00:00Z"),
                    false,
                    Instant.parse("2026-05-04T08:00:00Z")
                )
            )
        );

        EmsWorkspaceAnalysisService service = new EmsWorkspaceAnalysisService(
            Optional.empty(),
            Optional.empty(),
            authAccountStore,
            scrobbleStore,
            Optional.of(new FakeLastFmWebApiClient())
        );

        EmsWorkspaceAnalysisResponse response = service.analyzeWorkspace(
            new EmsWorkspaceAnalysisRequest(
                userId,
                null,
                List.of(),
                List.of(),
                List.of("dream-pop")
            )
        );

        assertThat(response.topSignals()).anyMatch(signal ->
            signal.type().equals("artist") && signal.label().equals("Tycho")
        );
        assertThat(response.notes()).anyMatch(note -> note.contains("Stored Last.fm scrobble snapshot"));
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
