package io.myforevermusic.api.modules.platform.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.myforevermusic.api.modules.auth.application.AuthRegistrationService;
import io.myforevermusic.api.modules.auth.infrastructure.local.InMemoryAuthAccountStore;
import io.myforevermusic.api.modules.auth.presentation.AuthRegistrationRequest;
import io.myforevermusic.api.modules.platform.infrastructure.local.InMemoryPlatformCredentialStore;
import io.myforevermusic.api.modules.platform.infrastructure.local.InMemoryPlatformConnectionStore;
import io.myforevermusic.api.modules.platform.presentation.PlatformConnectRequest;
import io.myforevermusic.api.modules.platform.presentation.PlatformDisconnectRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PlatformConnectionServiceTest {

    @Test
    void shouldReturnConnectedPreferredPlatformAfterConnect() {
        InMemoryAuthAccountStore authAccountStore = new InMemoryAuthAccountStore();
        InMemoryPlatformCredentialStore platformCredentialStore = new InMemoryPlatformCredentialStore();
        AuthRegistrationService authRegistrationService = new AuthRegistrationService(
            authAccountStore,
            new BCryptPasswordEncoder()
        );
        String userId = authRegistrationService.register(new AuthRegistrationRequest(
            "Forever Listener",
            "listener@example.com",
            "music2026",
            "spotify",
            false,
            true,
            true
        )).user().userId();

        PlatformConnectionService service = new PlatformConnectionService(
            authAccountStore,
            new PlatformCatalogService(),
            new InMemoryPlatformConnectionStore(),
            platformCredentialStore
        );

        service.connect(new PlatformConnectRequest(
            userId,
            "spotify",
            "sandbox",
            "Forever Listener Spotify"
        ));

        var response = service.getBootstrap(userId);

        assertThat(response.summary().preferredPlatformConnected()).isTrue();
        assertThat(response.summary().nextStepPath()).isEqualTo("/pms");
        assertThat(response.connections()).anyMatch(connection ->
            connection.platformId().equals("spotify") && connection.connected() && connection.syncReady()
        );
        assertThat(platformCredentialStore.findByUserIdAndPlatformId(userId, "spotify")).isPresent();
    }

    @Test
    void shouldClearCredentialAfterDisconnect() {
        InMemoryAuthAccountStore authAccountStore = new InMemoryAuthAccountStore();
        InMemoryPlatformCredentialStore platformCredentialStore = new InMemoryPlatformCredentialStore();
        AuthRegistrationService authRegistrationService = new AuthRegistrationService(
            authAccountStore,
            new BCryptPasswordEncoder()
        );
        String userId = authRegistrationService.register(new AuthRegistrationRequest(
            "Forever Listener",
            "listener@example.com",
            "music2026",
            "spotify",
            false,
            true,
            true
        )).user().userId();

        PlatformConnectionService service = new PlatformConnectionService(
            authAccountStore,
            new PlatformCatalogService(),
            new InMemoryPlatformConnectionStore(),
            platformCredentialStore
        );

        service.connect(new PlatformConnectRequest(userId, "spotify", "sandbox", null));
        service.disconnect(new PlatformDisconnectRequest(userId, "spotify"));

        assertThat(platformCredentialStore.findByUserIdAndPlatformId(userId, "spotify")).isEmpty();
    }
}
