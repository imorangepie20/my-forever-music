package io.myforevermusic.api.modules.platform.infrastructure.spotify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.myforevermusic.api.modules.platform.application.PlatformOAuthProperties;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SpotifyAppTokenServiceTest {

    @Test
    void shouldRequestAndCacheAppToken() throws IOException {
        AtomicInteger requestCount = new AtomicInteger(0);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/oauth/token", exchange -> {
            requestCount.incrementAndGet();
            String body = """
                {
                  "access_token": "app-token-abc",
                  "token_type": "Bearer",
                  "expires_in": 3600
                }
                """;
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();

        try {
            SpotifyAppTokenService service = service(server, Clock.fixed(Instant.parse("2026-05-17T10:00:00Z"), ZoneOffset.UTC));

            assertThat(service.getAccessToken()).isEqualTo("app-token-abc");
            assertThat(service.getAccessToken()).isEqualTo("app-token-abc");
            assertThat(requestCount.get()).isEqualTo(1);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldRefreshWhenTokenExpired() throws IOException {
        AtomicInteger requestCount = new AtomicInteger(0);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/oauth/token", exchange -> {
            int count = requestCount.incrementAndGet();
            String accessToken = "app-token-" + count;
            String body = """
                {
                  "access_token": "%s",
                  "token_type": "Bearer",
                  "expires_in": 60
                }
                """.formatted(accessToken);
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();

        try {
            MutableClock clock = new MutableClock(Instant.parse("2026-05-17T10:00:00Z"));
            SpotifyAppTokenService service = service(server, clock);

            assertThat(service.getAccessToken()).isEqualTo("app-token-1");
            // Token expires_in=60 with 30s refresh buffer → still valid after 25s, refresh after 35s.
            clock.advanceSeconds(25);
            assertThat(service.getAccessToken()).isEqualTo("app-token-1");
            clock.advanceSeconds(15);
            assertThat(service.getAccessToken()).isEqualTo("app-token-2");
            assertThat(requestCount.get()).isEqualTo(2);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldSurfaceClearErrorWhenSpotifyRejects() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/oauth/token", exchange -> {
            String body = """
                {
                  "error": "invalid_client",
                  "error_description": "Invalid client credentials"
                }
                """;
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(401, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();

        try {
            SpotifyAppTokenService service = service(server, Clock.fixed(Instant.parse("2026-05-17T10:00:00Z"), ZoneOffset.UTC));

            assertThatThrownBy(service::getAccessToken)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("401")
                .hasMessageContaining("Invalid client credentials");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldFailFastWhenNotConfigured() {
        PlatformOAuthProperties properties = new PlatformOAuthProperties();
        properties.getSpotify().setClientId("");
        properties.getSpotify().setClientSecret("");
        SpotifyAppTokenService service = new SpotifyAppTokenService(
            properties,
            new ObjectMapper(),
            HttpClient.newHttpClient(),
            Clock.fixed(Instant.parse("2026-05-17T10:00:00Z"), ZoneOffset.UTC)
        );

        assertThat(service.isConfigured()).isFalse();
        assertThatThrownBy(service::getAccessToken)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not configured");
    }

    private SpotifyAppTokenService service(HttpServer server, Clock clock) {
        PlatformOAuthProperties properties = new PlatformOAuthProperties();
        properties.getSpotify().setClientId("client-id");
        properties.getSpotify().setClientSecret("client-secret");
        properties.getSpotify().setTokenUri("http://127.0.0.1:%d/oauth/token".formatted(server.getAddress().getPort()));
        return new SpotifyAppTokenService(properties, new ObjectMapper(), HttpClient.newHttpClient(), clock);
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant initial) {
            this.now = initial;
        }

        void advanceSeconds(long seconds) {
            now = now.plusSeconds(seconds);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
