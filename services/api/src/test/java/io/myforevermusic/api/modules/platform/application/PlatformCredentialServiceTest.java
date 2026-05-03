package io.myforevermusic.api.modules.platform.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.myforevermusic.api.modules.platform.infrastructure.local.InMemoryPlatformCredentialStore;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlatformCredentialServiceTest {

    @Test
    void shouldRefreshExpiredSpotifyCredentialAndPreserveExistingRefreshToken() {
        InMemoryPlatformCredentialStore credentialStore = new InMemoryPlatformCredentialStore();
        PlatformAccountCredential expiredCredential = new PlatformAccountCredential(
            "user-001",
            "spotify",
            "spotify-pkce-draft",
            "spotify-user-001",
            "Forever Listener Spotify",
            "expired-access-token",
            "spotify-refresh-token",
            "Bearer",
            "playlist-read-private, user-read-email",
            Instant.now().minusSeconds(10),
            Instant.parse("2026-05-03T00:00:00Z"),
            Instant.parse("2026-05-03T00:00:00Z")
        );
        credentialStore.save(expiredCredential);

        PlatformTokenRefreshClient refreshClient = new PlatformTokenRefreshClient() {
            @Override
            public boolean supports(PlatformAccountCredential credential) {
                return "spotify".equals(credential.platformId());
            }

            @Override
            public PlatformTokenExchangeResult refreshAccessToken(PlatformAccountCredential credential) {
                return new PlatformTokenExchangeResult(
                    "refreshed-access-token",
                    "",
                    "Bearer",
                    List.of(),
                    Instant.now().plusSeconds(3600)
                );
            }
        };

        PlatformCredentialService service = new PlatformCredentialService(
            credentialStore,
            new PlatformTokenRefreshRegistry(List.of(refreshClient))
        );

        var refreshedCredential = service.findUsableCredential("user-001", "spotify").orElseThrow();

        assertThat(refreshedCredential.accessToken()).isEqualTo("refreshed-access-token");
        assertThat(refreshedCredential.refreshToken()).isEqualTo("spotify-refresh-token");
        assertThat(refreshedCredential.scopeSummary()).isEqualTo("playlist-read-private, user-read-email");
        assertThat(refreshedCredential.accessTokenExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void shouldMarkExpiredCredentialWithoutRefreshPathAsReconnectRequired() {
        InMemoryPlatformCredentialStore credentialStore = new InMemoryPlatformCredentialStore();
        credentialStore.save(new PlatformAccountCredential(
            "user-001",
            "spotify",
            "spotify-pkce-draft",
            "spotify-user-001",
            "Forever Listener Spotify",
            "expired-access-token",
            "",
            "Bearer",
            "playlist-read-private, user-read-email",
            Instant.now().minusSeconds(10),
            Instant.parse("2026-05-03T00:00:00Z"),
            Instant.parse("2026-05-03T00:00:00Z")
        ));

        PlatformCredentialService service = new PlatformCredentialService(
            credentialStore,
            new PlatformTokenRefreshRegistry(List.of())
        );

        PlatformCredentialResolution resolution = service.resolveCredential("user-001", "spotify");

        assertThat(resolution.usable()).isFalse();
        assertThat(resolution.reconnectRequired()).isTrue();
        assertThat(resolution.status()).isEqualTo(PlatformCredentialResolution.STATUS_RECONNECT_REQUIRED);
    }
}
