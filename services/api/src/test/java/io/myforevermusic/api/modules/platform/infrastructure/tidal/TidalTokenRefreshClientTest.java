package io.myforevermusic.api.modules.platform.infrastructure.tidal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.myforevermusic.api.modules.platform.application.PlatformAccountCredential;
import io.myforevermusic.api.modules.platform.application.PlatformOAuthProperties;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class TidalTokenRefreshClientTest {

    @Test
    void shouldRefreshTidalAccessTokenWithStoredRefreshToken() throws IOException {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/token", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = """
                {
                  "access_token": "tidal-refreshed-access-token",
                  "token_type": "Bearer",
                  "scope": "r_usr",
                  "expires_in": 86400
                }
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try {
            PlatformOAuthProperties properties = new PlatformOAuthProperties();
            properties.getTidal().setTokenUri("http://127.0.0.1:%d/token".formatted(server.getAddress().getPort()));
            TidalTokenRefreshClient client = new TidalTokenRefreshClient(properties, new ObjectMapper());
            PlatformAccountCredential credential = new PlatformAccountCredential(
                "user-001",
                "tidal",
                "tidal-pkce-draft",
                "tidal-user-001",
                "Forever Listener TIDAL",
                "expired-access-token",
                "tidal-refresh-token",
                "Bearer",
                "r_usr",
                Instant.now().minusSeconds(10),
                Instant.parse("2026-05-04T00:00:00Z"),
                Instant.parse("2026-05-04T00:00:00Z")
            );

            var result = client.refreshAccessToken(credential);

            assertThat(client.supports(credential)).isTrue();
            assertThat(result.accessToken()).isEqualTo("tidal-refreshed-access-token");
            assertThat(result.refreshToken()).isNull();
            assertThat(result.grantedScopes()).containsExactly("r_usr");
            assertThat(result.accessTokenExpiresAt()).isAfter(Instant.now());
            assertThat(requestBody.get()).isEqualTo("grant_type=refresh_token&refresh_token=tidal-refresh-token");
        } finally {
            server.stop(0);
        }
    }
}
