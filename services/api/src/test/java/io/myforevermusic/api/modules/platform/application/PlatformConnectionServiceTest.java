package io.myforevermusic.api.modules.platform.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.myforevermusic.api.modules.auth.application.AuthRegistrationService;
import io.myforevermusic.api.modules.auth.infrastructure.local.InMemoryAuthAccountStore;
import io.myforevermusic.api.modules.auth.presentation.AuthRegistrationRequest;
import io.myforevermusic.api.modules.platform.infrastructure.local.InMemoryPlatformCredentialStore;
import io.myforevermusic.api.modules.platform.infrastructure.local.InMemoryPlatformConnectionStore;
import io.myforevermusic.api.modules.platform.presentation.PlatformConnectRequest;
import io.myforevermusic.api.modules.platform.presentation.PlatformDisconnectRequest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PlatformConnectionServiceTest {

    @Test
    void shouldRejectDirectConnectForPmsPlatform() {
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
            platformCredentialStore,
            new PlatformCredentialService(
                platformCredentialStore,
                new PlatformTokenRefreshRegistry(List.of())
            )
        );

        assertThatThrownBy(() -> service.connect(new PlatformConnectRequest(
            userId,
            "spotify",
            "sandbox",
            "Forever Listener Spotify"
        )))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Direct platform connect is disabled");
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

        InMemoryPlatformConnectionStore platformConnectionStore = new InMemoryPlatformConnectionStore();
        PlatformConnectionService service = new PlatformConnectionService(
            authAccountStore,
            new PlatformCatalogService(),
            platformConnectionStore,
            platformCredentialStore,
            new PlatformCredentialService(
                platformCredentialStore,
                new PlatformTokenRefreshRegistry(List.of())
            )
        );

        platformConnectionStore.connect(new PlatformConnectionDraft(
            userId,
            "spotify",
            "spotify-pkce-draft",
            "Forever Listener Spotify",
            "playlist-read-private",
            true,
            Instant.parse("2026-05-03T00:00:00Z"),
            Instant.parse("2026-05-03T00:00:00Z")
        ));
        platformCredentialStore.save(new PlatformAccountCredential(
            userId,
            "spotify",
            "spotify-pkce-draft",
            "spotify-user-001",
            "Forever Listener Spotify",
            "access-token",
            "refresh-token",
            "Bearer",
            "playlist-read-private",
            Instant.now().plusSeconds(3600),
            Instant.parse("2026-05-03T00:00:00Z"),
            Instant.parse("2026-05-03T00:00:00Z")
        ));
        service.disconnect(new PlatformDisconnectRequest(userId, "spotify"));

        assertThat(platformCredentialStore.findByUserIdAndPlatformId(userId, "spotify")).isEmpty();
    }

    @Test
    void shouldMarkPreferredPlatformAsReconnectRequiredWhenCredentialCannotBeRefreshed() {
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

        InMemoryPlatformConnectionStore platformConnectionStore = new InMemoryPlatformConnectionStore();
        PlatformConnectionService service = new PlatformConnectionService(
            authAccountStore,
            new PlatformCatalogService(),
            platformConnectionStore,
            platformCredentialStore,
            new PlatformCredentialService(
                platformCredentialStore,
                new PlatformTokenRefreshRegistry(List.of())
            )
        );

        platformConnectionStore.connect(new PlatformConnectionDraft(
            userId,
            "spotify",
            "spotify-pkce-draft",
            "Forever Listener Spotify",
            "playlist-read-private",
            true,
            Instant.parse("2026-05-03T00:00:00Z"),
            Instant.parse("2026-05-03T00:00:00Z")
        ));
        platformCredentialStore.save(new PlatformAccountCredential(
            userId,
            "spotify",
            "spotify-pkce-draft",
            "spotify-user-001",
            "Forever Listener Spotify",
            "expired-access-token",
            "",
            "Bearer",
            "playlist-read-private",
            Instant.now().minusSeconds(30),
            Instant.parse("2026-05-03T00:00:00Z"),
            Instant.parse("2026-05-03T00:00:00Z")
        ));

        var response = service.getBootstrap(userId);

        assertThat(response.summary().preferredPlatformConnected()).isFalse();
        assertThat(response.summary().preferredPlatformReconnectRequired()).isTrue();
        assertThat(response.connections()).anyMatch(connection ->
            connection.platformId().equals("spotify")
                && connection.connected()
                && connection.reconnectRequired()
                && "Reconnect".equals(connection.nextActionLabel())
        );
    }

    @Test
    void shouldRejectDirectConnectForLastFmSignalPlatform() {
        InMemoryAuthAccountStore authAccountStore = new InMemoryAuthAccountStore();
        InMemoryPlatformCredentialStore platformCredentialStore = new InMemoryPlatformCredentialStore();
        AuthRegistrationService authRegistrationService = new AuthRegistrationService(
            authAccountStore,
            new BCryptPasswordEncoder()
        );
        String userId = authRegistrationService.register(new AuthRegistrationRequest(
            "Forever Listener",
            "lastfm-listener@example.com",
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
            platformCredentialStore,
            new PlatformCredentialService(
                platformCredentialStore,
                new PlatformTokenRefreshRegistry(List.of())
            )
        );

        assertThatThrownBy(() -> service.connect(new PlatformConnectRequest(userId, "last-fm", "sandbox", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Direct platform connect is disabled");
    }
}
