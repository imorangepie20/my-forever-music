package io.myforevermusic.api.modules.platform.infrastructure.spotify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.myforevermusic.api.modules.platform.application.PlatformAuthorizationCodeExchangeClient;
import io.myforevermusic.api.modules.platform.application.PlatformAuthorizationSession;
import io.myforevermusic.api.modules.platform.application.PlatformOAuthProperties;
import io.myforevermusic.api.modules.platform.application.PlatformTokenExchangeResult;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SpotifyAuthorizationCodeExchangeClient implements PlatformAuthorizationCodeExchangeClient {

    private final PlatformOAuthProperties platformOAuthProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public SpotifyAuthorizationCodeExchangeClient(
        PlatformOAuthProperties platformOAuthProperties,
        ObjectMapper objectMapper
    ) {
        this(platformOAuthProperties, objectMapper, HttpClient.newHttpClient());
    }

    SpotifyAuthorizationCodeExchangeClient(
        PlatformOAuthProperties platformOAuthProperties,
        ObjectMapper objectMapper,
        HttpClient httpClient
    ) {
        this.platformOAuthProperties = platformOAuthProperties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public boolean supports(PlatformAuthorizationSession session) {
        return "spotify-pkce-draft".equals(session.authorizationMode());
    }

    @Override
    public PlatformTokenExchangeResult exchangeAuthorizationCode(
        PlatformAuthorizationSession session,
        String authorizationCode
    ) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(platformOAuthProperties.getSpotify().getTokenUri()))
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(buildFormBody(session, authorizationCode)))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalArgumentException(readErrorMessage(response.body(), response.statusCode()));
            }

            SpotifyTokenResponse payload = objectMapper.readValue(response.body(), SpotifyTokenResponse.class);
            Instant expiresAt = payload.expiresIn() == null
                ? null
                : Instant.now().plusSeconds(payload.expiresIn());

            List<String> grantedScopes = payload.scope() == null || payload.scope().isBlank()
                ? session.requestedScopes()
                : List.of(payload.scope().trim().split("\\s+"));

            return new PlatformTokenExchangeResult(
                payload.accessToken(),
                payload.refreshToken(),
                payload.tokenType() == null || payload.tokenType().isBlank() ? "Bearer" : payload.tokenType(),
                grantedScopes,
                expiresAt
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Spotify token response could not be parsed.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Spotify token exchange was interrupted.", exception);
        }
    }

    private String buildFormBody(PlatformAuthorizationSession session, String authorizationCode) {
        return "grant_type=authorization_code"
            + "&code=" + encode(authorizationCode)
            + "&redirect_uri=" + encode(session.redirectUri())
            + "&client_id=" + encode(platformOAuthProperties.getSpotify().getClientId())
            + "&code_verifier=" + encode(session.pkceCodeVerifier());
    }

    private String readErrorMessage(String body, int statusCode) {
        try {
            SpotifyTokenErrorResponse error = objectMapper.readValue(body, SpotifyTokenErrorResponse.class);
            if (error.errorDescription() != null && !error.errorDescription().isBlank()) {
                return "Spotify token exchange failed (%s): %s".formatted(statusCode, error.errorDescription());
            }
            if (error.error() != null && !error.error().isBlank()) {
                return "Spotify token exchange failed (%s): %s".formatted(statusCode, error.error());
            }
        } catch (IOException ignored) {
            // Fall through to generic response below.
        }

        return body == null || body.isBlank()
            ? "Spotify token exchange failed with status %s.".formatted(statusCode)
            : "Spotify token exchange failed (%s): %s".formatted(statusCode, body);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpotifyTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("scope") String scope,
        @JsonProperty("expires_in") Long expiresIn,
        @JsonProperty("refresh_token") String refreshToken
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpotifyTokenErrorResponse(
        @JsonProperty("error") String error,
        @JsonProperty("error_description") String errorDescription
    ) {
    }
}
