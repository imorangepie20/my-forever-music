package io.myforevermusic.api.modules.platform.infrastructure.spotify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.myforevermusic.api.modules.platform.application.PlatformOAuthProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Issues and caches a Spotify app-only access token via the OAuth Client
 * Credentials grant. Used for endpoints that do not require a user context
 * (search, public track lookup) so unauthenticated users can still drive the
 * main page hero banner.
 */
@Component
public class SpotifyAppTokenService {

    private static final Logger log = LoggerFactory.getLogger(SpotifyAppTokenService.class);
    private static final long REFRESH_BUFFER_SECONDS = 30;

    private final PlatformOAuthProperties platformOAuthProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Clock clock;

    private final AtomicReference<CachedToken> cached = new AtomicReference<>();
    private final ReentrantLock refreshLock = new ReentrantLock();

    @Autowired
    public SpotifyAppTokenService(
        PlatformOAuthProperties platformOAuthProperties,
        ObjectMapper objectMapper
    ) {
        this(platformOAuthProperties, objectMapper, HttpClient.newHttpClient(), Clock.systemUTC());
    }

    SpotifyAppTokenService(
        PlatformOAuthProperties platformOAuthProperties,
        ObjectMapper objectMapper,
        HttpClient httpClient,
        Clock clock
    ) {
        this.platformOAuthProperties = platformOAuthProperties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.clock = clock;
    }

    public boolean isConfigured() {
        PlatformOAuthProperties.Spotify spotify = platformOAuthProperties.getSpotify();
        return spotify.getClientId() != null
            && !spotify.getClientId().isBlank()
            && spotify.getClientSecret() != null
            && !spotify.getClientSecret().isBlank()
            && spotify.getTokenUri() != null
            && !spotify.getTokenUri().isBlank();
    }

    public String getAccessToken() {
        CachedToken token = cached.get();
        if (token != null && token.isValid(clock.instant())) {
            return token.accessToken;
        }
        refreshLock.lock();
        try {
            token = cached.get();
            if (token != null && token.isValid(clock.instant())) {
                return token.accessToken;
            }
            CachedToken refreshed = requestAppToken();
            cached.set(refreshed);
            return refreshed.accessToken;
        } finally {
            refreshLock.unlock();
        }
    }

    public void invalidateCache() {
        cached.set(null);
    }

    private CachedToken requestAppToken() {
        PlatformOAuthProperties.Spotify spotify = platformOAuthProperties.getSpotify();
        if (!isConfigured()) {
            throw new IllegalStateException(
                "Spotify client credentials are not configured. Set SPOTIFY_CLIENT_ID and SPOTIFY_CLIENT_SECRET."
            );
        }

        String basic = Base64.getEncoder().encodeToString(
            ("%s:%s".formatted(spotify.getClientId(), spotify.getClientSecret()))
                .getBytes(StandardCharsets.UTF_8)
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(spotify.getTokenUri()))
            .header("Accept", "application/json")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Authorization", "Basic " + basic)
            .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(readErrorMessage(response.body(), response.statusCode()));
            }

            SpotifyAppTokenResponse payload = objectMapper.readValue(response.body(), SpotifyAppTokenResponse.class);
            if (payload.accessToken() == null || payload.accessToken().isBlank()) {
                throw new IllegalStateException("Spotify client credentials response is missing access_token.");
            }

            long expiresIn = payload.expiresIn() == null ? 3600L : Math.max(60L, payload.expiresIn());
            Instant expiresAt = clock.instant().plusSeconds(expiresIn);
            log.debug("Spotify app token issued. expires_at={}", expiresAt);
            return new CachedToken(payload.accessToken(), expiresAt);
        } catch (IOException exception) {
            throw new IllegalStateException("Spotify client credentials response could not be parsed.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Spotify client credentials request was interrupted.", exception);
        }
    }

    private String readErrorMessage(String body, int statusCode) {
        try {
            SpotifyAppTokenErrorResponse error = objectMapper.readValue(body, SpotifyAppTokenErrorResponse.class);
            if (error.errorDescription() != null && !error.errorDescription().isBlank()) {
                return "Spotify client credentials request failed (%s): %s".formatted(statusCode, error.errorDescription());
            }
            if (error.error() != null && !error.error().isBlank()) {
                return "Spotify client credentials request failed (%s): %s".formatted(statusCode, error.error());
            }
        } catch (IOException ignored) {
            // fall through
        }
        return body == null || body.isBlank()
            ? "Spotify client credentials request failed with status %s.".formatted(statusCode)
            : "Spotify client credentials request failed (%s): %s".formatted(statusCode, body);
    }

    private record CachedToken(String accessToken, Instant expiresAt) {
        boolean isValid(Instant now) {
            return now.plusSeconds(REFRESH_BUFFER_SECONDS).isBefore(expiresAt);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpotifyAppTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") Long expiresIn
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpotifyAppTokenErrorResponse(
        @JsonProperty("error") String error,
        @JsonProperty("error_description") String errorDescription
    ) {
    }
}
