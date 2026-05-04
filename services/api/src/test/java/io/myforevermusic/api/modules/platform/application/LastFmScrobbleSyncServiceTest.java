package io.myforevermusic.api.modules.platform.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.myforevermusic.api.modules.auth.application.AuthRegistrationService;
import io.myforevermusic.api.modules.auth.infrastructure.local.InMemoryAuthAccountStore;
import io.myforevermusic.api.modules.auth.presentation.AuthRegistrationRequest;
import io.myforevermusic.api.modules.platform.infrastructure.lastfm.LastFmWebApiClient;
import io.myforevermusic.api.modules.platform.infrastructure.local.InMemoryLastFmScrobbleStore;
import io.myforevermusic.api.modules.platform.presentation.LastFmScrobbleSyncRequest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class LastFmScrobbleSyncServiceTest {

    @Test
    void shouldSyncAndStoreRecentScrobbles() {
        InMemoryAuthAccountStore authAccountStore = new InMemoryAuthAccountStore();
        AuthRegistrationService authRegistrationService = new AuthRegistrationService(
            authAccountStore,
            new BCryptPasswordEncoder()
        );
        String userId = authRegistrationService.register(new AuthRegistrationRequest(
            "Forever Listener",
            "scrobble-sync@example.com",
            "music2026",
            "spotify",
            false,
            true,
            true
        )).user().userId();
        authAccountStore.saveLastFmProfile(userId, "mibeen", Instant.parse("2026-05-04T00:00:00Z"));

        LastFmScrobbleSyncService service = new LastFmScrobbleSyncService(
            authAccountStore,
            new InMemoryLastFmScrobbleStore(),
            new FakeLastFmWebApiClient()
        );

        var response = service.syncScrobbles(new LastFmScrobbleSyncRequest(userId, 40));
        var bootstrap = service.getBootstrap(userId);

        assertThat(response.status()).isEqualTo("synced");
        assertThat(response.sync().insertedScrobbleCount()).isEqualTo(2);
        assertThat(response.sync().skippedNowPlayingCount()).isEqualTo(1);
        assertThat(response.recentScrobbles()).hasSize(2);
        assertThat(bootstrap.summary().storedScrobbleCount()).isEqualTo(2);
        assertThat(bootstrap.recentScrobbles()).extracting(item -> item.artistName())
            .containsExactly("The Midnight", "M83");
    }

    private static final class FakeLastFmWebApiClient extends LastFmWebApiClient {

        private FakeLastFmWebApiClient() {
            super(new LastFmProperties(), new ObjectMapper());
        }

        @Override
        public List<LastFmRecentTrack> getRecentTracks(String username, int limit) {
            return List.of(
                new LastFmRecentTrack(
                    "Days of Thunder",
                    "The Midnight",
                    "Days of Thunder",
                    "https://www.last.fm/music/The+Midnight/_/Days+of+Thunder",
                    null,
                    false,
                    Instant.parse("2026-05-03T20:00:00Z"),
                    true
                ),
                new LastFmRecentTrack(
                    "Midnight City",
                    "M83",
                    "Hurry Up, We're Dreaming",
                    "https://www.last.fm/music/M83/_/Midnight+City",
                    null,
                    false,
                    Instant.parse("2026-05-03T18:00:00Z"),
                    false
                ),
                new LastFmRecentTrack(
                    "Current Loop",
                    "Live Artist",
                    "Current Album",
                    "https://www.last.fm/music/Live+Artist/_/Current+Loop",
                    null,
                    true,
                    null,
                    false
                )
            );
        }
    }
}
