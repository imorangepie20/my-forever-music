package io.myforevermusic.api.modules.platform.infrastructure.spotify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.myforevermusic.api.modules.platform.application.PlatformAccountCredential;
import io.myforevermusic.api.modules.platform.application.PlatformOAuthProperties;
import io.myforevermusic.api.modules.platform.application.PlatformTokenExchangeResult;
import io.myforevermusic.api.modules.platform.application.PlatformTokenRefreshClient;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SpotifyTokenRefreshClient implements PlatformTokenRefreshClient {

    private final PlatformOAuthProperties platformOAuthProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public SpotifyTokenRefreshClient(
        PlatformOAuthProperties platformOAuthProperties,
        ObjectMapper objectMapper
    ) {
        this(platformOAuthProperties, objectMapper, HttpClient.newHttpClient());
    }

    SpotifyTokenRefreshClient(
        PlatformOAuthProperties platformOAuthProperties,
        ObjectMapper objectMapper,
        HttpClient httpClient
    ) {
        this.platformOAuthProperties = platformOAuthProperties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public boolean supports(PlatformAccountCredential credential) {
        return "spotify".equals(credential.platformId())
            && credential.refreshToken() != null
            && !credential.refreshToken().isBlank()
            && credential.authorizationMode() != null
            && credential.authorizationMode().startsWith("spotify");
    }

    @Override
    public PlatformTokenExchangeResult refreshAccessToken(PlatformAccountCredential credential) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(platformOAuthProperties.getSpotify().getTokenUri()))
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded");

            String formBody = buildFormBody(credential);
            if (requiresBasicAuthorization(credential)) {
                requestBuilder.header("Authorization", "Basic %s".formatted(
                    Base64.getEncoder().encodeToString((
                        platformOAuthProperties.getSpotify().getClientId()
                            + ":"
                            + platformOAuthProperties.getSpotify().getClientSecret()
                    ).getBytes(StandardCharsets.UTF_8))
                ));
            }

            HttpRequest request = requestBuilder
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalArgumentException(readErrorMessage(response.body(), response.statusCode()));
            }

            SpotifyRefreshTokenResponse payload = objectMapper.readValue(response.body(), SpotifyRefreshTokenResponse.class);
            Instant expiresAt = payload.expiresIn() == null
                ? null
                : Instant.now().plusSeconds(payload.expiresIn());
            List<String> grantedScopes = payload.scope() == null || payload.scope().isBlank()
                ? List.of()
                : List.of(payload.scope().trim().split("\\s+"));

            return new PlatformTokenExchangeResult(
                payload.accessToken(),
                payload.refreshToken(),
                payload.tokenType() == null || payload.tokenType().isBlank() ? "Bearer" : payload.tokenType(),
                grantedScopes,
                expiresAt
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Spotify refresh token response could not be parsed.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Spotify token refresh was interrupted.", exception);
        }
    }

    private String buildFormBody(PlatformAccountCredential credential) {
        String body = "grant_type=refresh_token"
            + "&refresh_token=" + encode(credential.refreshToken());
        if (!requiresBasicAuthorization(credential)) {
            body += "&client_id=" + encode(platformOAuthProperties.getSpotify().getClientId());
        }
        return body;
    }

    private boolean requiresBasicAuthorization(PlatformAccountCredential credential) {
        return credential.authorizationMode() != null
            && !credential.authorizationMode().contains("pkce")
            && platformOAuthProperties.getSpotify().getClientSecret() != null
            && !platformOAuthProperties.getSpotify().getClientSecret().isBlank();
    }

    private String readErrorMessage(String body, int statusCode) {
        try {
            SpotifyRefreshTokenErrorResponse error = objectMapper.readValue(body, SpotifyRefreshTokenErrorResponse.class);
            if (error.errorDescription() != null && !error.errorDescription().isBlank()) {
                return "Spotify token refresh failed (%s): %s".formatted(statusCode, error.errorDescription());
            }
            if (error.error() != null && !error.error().isBlank()) {
                return "Spotify token refresh failed (%s): %s".formatted(statusCode, error.error());
            }
        } catch (IOException ignored) {
            // Fall through to generic response below.
        }

        return body == null || body.isBlank()
            ? "Spotify token refresh failed with status %s.".formatted(statusCode)
            : "Spotify token refresh failed (%s): %s".formatted(statusCode, body);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpotifyRefreshTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("scope") String scope,
        @JsonProperty("expires_in") Long expiresIn,
        @JsonProperty("refresh_token") String refreshToken
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpotifyRefreshTokenErrorResponse(
        @JsonProperty("error") String error,
        @JsonProperty("error_description") String errorDescription
    ) {
    }
}
