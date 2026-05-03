package io.myforevermusic.api.modules.platform.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.myforevermusic.api.modules.auth.application.AuthRegistrationService;
import io.myforevermusic.api.modules.auth.infrastructure.local.InMemoryAuthAccountStore;
import io.myforevermusic.api.modules.auth.presentation.AuthRegistrationRequest;
import io.myforevermusic.api.modules.platform.infrastructure.local.InMemoryPlatformAuthorizationSessionStore;
import io.myforevermusic.api.modules.platform.infrastructure.local.InMemoryPlatformCredentialStore;
import io.myforevermusic.api.modules.platform.infrastructure.local.InMemoryPlatformConnectionStore;
import io.myforevermusic.api.modules.platform.infrastructure.sandbox.SandboxAuthorizationCodeExchangeClient;
import io.myforevermusic.api.modules.platform.presentation.PlatformAuthorizationCompleteRequest;
import io.myforevermusic.api.modules.platform.presentation.PlatformAuthorizationStartRequest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PlatformAuthorizationServiceTest {

    @Test
    void shouldStartAndCompleteSandboxAuthorization() {
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

        PlatformAuthorizationService service = new PlatformAuthorizationService(
            authAccountStore,
            new PlatformCatalogService(),
            new InMemoryPlatformAuthorizationSessionStore(),
            new InMemoryPlatformConnectionStore(),
            platformCredentialStore,
            new PlatformAuthorizationCodeExchangeRegistry(List.of(new SandboxAuthorizationCodeExchangeClient())),
            new PlatformOAuthProperties()
        );

        var start = service.startAuthorization(new PlatformAuthorizationStartRequest(userId, "spotify"));
        var complete = service.completeAuthorization(
            new PlatformAuthorizationCompleteRequest(
                userId,
                "spotify",
                start.authorization().state(),
                start.authorization().sandboxApprovalCode(),
                null
            )
        );

        assertThat(start.status()).isEqualTo("authorization_pending");
        assertThat(start.authorization().approvalPagePath()).contains("/platforms/oauth/authorize");
        assertThat(complete.status()).isEqualTo("authorization_completed");
        assertThat(complete.connection().connected()).isTrue();
        assertThat(complete.nextStep().path()).isEqualTo("/pms");
        assertThat(platformCredentialStore.findByUserIdAndPlatformId(userId, "spotify")).isPresent();
    }

    @Test
    void shouldStartSpotifyPkceDraftAndStoreExchangedToken() {
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

        PlatformOAuthProperties properties = new PlatformOAuthProperties();
        properties.getSpotify().setEnabled(true);
        properties.getSpotify().setClientId("spotify-client-id");
        properties.getSpotify().setRedirectUri("http://localhost:5173/platforms/oauth/callback");

        PlatformAuthorizationCodeExchangeClient fakeSpotifyClient = new PlatformAuthorizationCodeExchangeClient() {
            @Override
            public boolean supports(PlatformAuthorizationSession session) {
                return "spotify-pkce-draft".equals(session.authorizationMode());
            }

            @Override
            public PlatformTokenExchangeResult exchangeAuthorizationCode(
                PlatformAuthorizationSession session,
                String authorizationCode
            ) {
                return new PlatformTokenExchangeResult(
                    "spotify-access-token",
                    "spotify-refresh-token",
                    "Bearer",
                    List.of("user-read-email", "playlist-read-private"),
                    Instant.parse("2026-05-04T00:00:00Z")
                );
            }
        };

        PlatformAuthorizationService service = new PlatformAuthorizationService(
            authAccountStore,
            new PlatformCatalogService(),
            new InMemoryPlatformAuthorizationSessionStore(),
            new InMemoryPlatformConnectionStore(),
            platformCredentialStore,
            new PlatformAuthorizationCodeExchangeRegistry(
                List.of(new SandboxAuthorizationCodeExchangeClient(), fakeSpotifyClient)
            ),
            properties
        );

        var start = service.startAuthorization(new PlatformAuthorizationStartRequest(userId, "spotify"));
        var complete = service.completeAuthorization(
            new PlatformAuthorizationCompleteRequest(
                userId,
                "spotify",
                start.authorization().state(),
                null,
                "spotify-auth-code"
            )
        );
        var credential = platformCredentialStore.findByUserIdAndPlatformId(userId, "spotify").orElseThrow();

        assertThat(start.authorization().authorizationMode()).isEqualTo("spotify-pkce-draft");
        assertThat(start.authorization().authorizationChannel()).isEqualTo("external_browser_redirect");
        assertThat(start.authorization().externalAuthorizationUrl()).contains("accounts.spotify.com/authorize");
        assertThat(complete.status()).isEqualTo("authorization_completed");
        assertThat(credential.accessToken()).isEqualTo("spotify-access-token");
        assertThat(credential.refreshToken()).isEqualTo("spotify-refresh-token");
        assertThat(credential.scopeSummary()).isEqualTo("user-read-email, playlist-read-private");
    }
}
