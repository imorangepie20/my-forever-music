package io.myforevermusic.api.modules.platform.infrastructure.tidal;

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
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TidalTokenRefreshClient implements PlatformTokenRefreshClient {

    private final PlatformOAuthProperties platformOAuthProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public TidalTokenRefreshClient(
        PlatformOAuthProperties platformOAuthProperties,
        ObjectMapper objectMapper
    ) {
        this(platformOAuthProperties, objectMapper, HttpClient.newHttpClient());
    }

    TidalTokenRefreshClient(
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
        return "tidal".equals(credential.platformId())
            && credential.refreshToken() != null
            && !credential.refreshToken().isBlank()
            && credential.authorizationMode() != null
            && credential.authorizationMode().startsWith("tidal");
    }

    @Override
    public PlatformTokenExchangeResult refreshAccessToken(PlatformAccountCredential credential) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(platformOAuthProperties.getTidal().getTokenUri()))
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(buildFormBody(credential)))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalArgumentException(readErrorMessage(response.body(), response.statusCode()));
            }

            TidalRefreshTokenResponse payload = objectMapper.readValue(response.body(), TidalRefreshTokenResponse.class);
            Instant expiresAt = payload.expiresIn() == null
                ? null
                : Instant.now().plusSeconds(payload.expiresIn());
            List<String> grantedScopes = payload.scope() == null || payload.scope().isBlank()
                ? List.of()
                : parseScopes(payload.scope());

            if (payload.accessToken() == null || payload.accessToken().isBlank()) {
                throw new IllegalArgumentException("TIDAL token refresh did not return an access token.");
            }

            return new PlatformTokenExchangeResult(
                payload.accessToken(),
                payload.refreshToken(),
                payload.tokenType() == null || payload.tokenType().isBlank() ? "Bearer" : payload.tokenType(),
                grantedScopes,
                expiresAt
            );
        } catch (IOException exception) {
            throw new IllegalStateException("TIDAL refresh token response could not be parsed.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("TIDAL token refresh was interrupted.", exception);
        }
    }

    private String buildFormBody(PlatformAccountCredential credential) {
        return "grant_type=refresh_token"
            + "&refresh_token=" + encode(credential.refreshToken());
    }

    private String readErrorMessage(String body, int statusCode) {
        try {
            TidalRefreshTokenErrorResponse error = objectMapper.readValue(body, TidalRefreshTokenErrorResponse.class);
            if (error.errorDescription() != null && !error.errorDescription().isBlank()) {
                return "TIDAL token refresh failed (%s): %s".formatted(statusCode, error.errorDescription());
            }
            if (error.error() != null && !error.error().isBlank()) {
                return "TIDAL token refresh failed (%s): %s".formatted(statusCode, error.error());
            }
        } catch (IOException ignored) {
            // Fall through to generic response below.
        }

        return body == null || body.isBlank()
            ? "TIDAL token refresh failed with status %s.".formatted(statusCode)
            : "TIDAL token refresh failed (%s): %s".formatted(statusCode, body);
    }

    private List<String> parseScopes(String scope) {
        return Arrays.stream(scope.trim().split("[,\\s]+"))
            .filter(value -> !value.isBlank())
            .toList();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TidalRefreshTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("scope") String scope,
        @JsonProperty("expires_in") Long expiresIn,
        @JsonProperty("refresh_token") String refreshToken
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TidalRefreshTokenErrorResponse(
        @JsonProperty("error") String error,
        @JsonProperty("error_description") String errorDescription
    ) {
    }
}
