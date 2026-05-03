package io.myforevermusic.api.modules.platform.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.myforevermusic.api.modules.auth.application.AuthRegistrationService;
import io.myforevermusic.api.modules.auth.infrastructure.local.InMemoryAuthAccountStore;
import io.myforevermusic.api.modules.auth.presentation.AuthRegistrationRequest;
import io.myforevermusic.api.modules.platform.infrastructure.lastfm.LastFmWebApiClient;
import io.myforevermusic.api.modules.platform.infrastructure.local.InMemoryPlatformConnectionStore;
import io.myforevermusic.api.modules.platform.presentation.LastFmProfileConnectRequest;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class LastFmProfileConnectionServiceTest {

    @Test
    void shouldSaveLastFmUsernameAndCreateAnalysisConnection() {
        InMemoryAuthAccountStore authAccountStore = new InMemoryAuthAccountStore();
        AuthRegistrationService authRegistrationService = new AuthRegistrationService(
            authAccountStore,
            new BCryptPasswordEncoder()
        );
        String userId = authRegistrationService.register(new AuthRegistrationRequest(
            "Forever Listener",
            "listener-lastfm@example.com",
            "music2026",
            "last-fm",
            false,
            true,
            true
        )).user().userId();

        InMemoryPlatformConnectionStore platformConnectionStore = new InMemoryPlatformConnectionStore();
        LastFmProfileConnectionService service = new LastFmProfileConnectionService(
            authAccountStore,
            new PlatformCatalogService(),
            platformConnectionStore,
            new FakeLastFmWebApiClient()
        );

        var response = service.connectProfile(new LastFmProfileConnectRequest(userId, "mibeen"));
        var account = authAccountStore.findByUserId(userId).orElseThrow();
        var states = platformConnectionStore.findByUserId(userId);

        assertThat(response.status()).isEqualTo("connected");
        assertThat(response.connection().platformId()).isEqualTo("last-fm");
        assertThat(response.connection().connectionMode()).isEqualTo("public-profile");
        assertThat(response.connection().externalAccountLabel()).isEqualTo("mibeen");
        assertThat(account.lastFmUsername()).isEqualTo("mibeen");
        assertThat(account.lastFmConnectedAt()).isNotNull();
        assertThat(states).hasSize(1);
        assertThat(states.getFirst().platformId()).isEqualTo("last-fm");
    }

    private static final class FakeLastFmWebApiClient extends LastFmWebApiClient {

        private FakeLastFmWebApiClient() {
            super(new LastFmProperties(), new ObjectMapper());
        }

        @Override
        public LastFmUserProfile getUserProfile(String username) {
            return new LastFmUserProfile(
                username,
                "Woo Sung Jo",
                "KR",
                54189L,
                "https://www.last.fm/user/%s".formatted(username),
                null,
                Instant.parse("2020-01-01T00:00:00Z")
            );
        }
    }
}
