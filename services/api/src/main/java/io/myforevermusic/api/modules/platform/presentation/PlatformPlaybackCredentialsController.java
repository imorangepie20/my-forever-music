package io.myforevermusic.api.modules.platform.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.myforevermusic.api.modules.platform.application.PlatformAccountCredential;
import io.myforevermusic.api.modules.platform.application.PlatformAccountProfile;
import io.myforevermusic.api.modules.platform.application.PlatformAccountProfileResolverRegistry;
import io.myforevermusic.api.modules.platform.application.PlatformCredentialResolution;
import io.myforevermusic.api.modules.platform.application.PlatformCredentialService;
import io.myforevermusic.api.modules.platform.application.PlatformOAuthProperties;
import io.myforevermusic.api.modules.platform.application.PlatformReconnectRequiredException;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platforms/playback")
@Validated
public class PlatformPlaybackCredentialsController {

    private static final List<String> REQUIRED_SPOTIFY_PLAYBACK_SCOPES = List.of(
        "streaming",
        "user-read-playback-state",
        "user-modify-playback-state"
    );
    private static final List<String> REQUIRED_TIDAL_PLAYBACK_SCOPES = List.of(
        "playback",
        "entitlements.read"
    );

    private final PlatformCredentialService platformCredentialService;
    private final PlatformAccountProfileResolverRegistry platformAccountProfileResolverRegistry;
    private final PlatformOAuthProperties platformOAuthProperties;
    private final ObjectMapper objectMapper;

    public PlatformPlaybackCredentialsController(
        PlatformCredentialService platformCredentialService,
        PlatformAccountProfileResolverRegistry platformAccountProfileResolverRegistry,
        PlatformOAuthProperties platformOAuthProperties,
        ObjectMapper objectMapper
    ) {
        this.platformCredentialService = platformCredentialService;
        this.platformAccountProfileResolverRegistry = platformAccountProfileResolverRegistry;
        this.platformOAuthProperties = platformOAuthProperties;
        this.objectMapper = objectMapper;
    }

    @Operation(summary = "Get a usable platform access token for browser playback")
    @GetMapping("/credentials")
    public PlatformPlaybackCredentialsResponse getCredentials(
        @RequestParam("user_id") @NotBlank String userId,
        @RequestParam("platform_id") @NotBlank String platformId
    ) {
        PlatformCredentialResolution resolution = platformCredentialService.resolveCredential(userId, platformId);
        if (PlatformCredentialResolution.STATUS_MISSING.equals(resolution.status())) {
            throw new IllegalArgumentException(
                "No stored credential exists for %s. Connect the platform before starting playback.".formatted(platformId)
            );
        }
        if (!resolution.usable()) {
            throw new PlatformReconnectRequiredException(
                platformId,
                resolution.detail() == null || resolution.detail().isBlank()
                    ? "Reconnect %s before starting playback.".formatted(platformId)
                    : resolution.detail()
            );
        }

        PlatformAccountCredential credential = resolution.credential();
        List<String> scopes = parseScopes(credential.scopeSummary());
        List<String> missingScopes = missingPlaybackScopes(platformId, scopes);
        if (!missingScopes.isEmpty()) {
            throw new PlatformReconnectRequiredException(
                platformId,
                "%s token is missing playback scopes: %s. Reconnect %s to grant playback access."
                    .formatted(platformId, String.join(", ", missingScopes), platformId)
            );
        }
        PlatformAccountProfile profile = platformAccountProfileResolverRegistry
            .resolve(credential)
            .orElse(null);
        String externalUserId = firstNonBlank(
            profile == null ? null : profile.externalUserId(),
            credential.externalUserId(),
            credential.userId()
        );
        String externalAccountLabel = firstNonBlank(
            profile == null ? null : profile.externalAccountLabel(),
            credential.externalAccountLabel()
        );
        if (requiresProviderPlaybackUserId(credential.platformId())
            && isLocalFallbackExternalUserId(credential.platformId(), externalUserId, credential.userId())) {
            throw new PlatformReconnectRequiredException(
                credential.platformId(),
                "TIDAL playback account id resolved to a local fallback instead of a provider account id. Reconnect TIDAL so the browser SDK receives the provider account id."
            );
        }

        return new PlatformPlaybackCredentialsResponse(
            "api",
            "ok",
            Instant.now(),
            credential.userId(),
            credential.platformId(),
            credential.accessToken(),
            credential.tokenType(),
            credential.scopeSummary(),
            scopes,
            credential.accessTokenExpiresAt(),
            externalUserId,
            externalAccountLabel,
            credential.authorizationMode(),
            playbackClientId(credential.platformId()),
            playbackCountryCode(credential)
        );
    }

    private List<String> parseScopes(String scopeSummary) {
        if (scopeSummary == null || scopeSummary.isBlank()) {
            return List.of();
        }

        return Arrays.stream(scopeSummary.split("[,\\s]+"))
            .map(String::trim)
            .filter(scope -> !scope.isBlank())
            .distinct()
            .toList();
    }

    private List<String> missingPlaybackScopes(String platformId, List<String> scopes) {
        if (!"spotify".equals(platformId)) {
            if ("tidal".equals(platformId)) {
                return REQUIRED_TIDAL_PLAYBACK_SCOPES.stream()
                    .filter(requiredScope -> !scopes.contains(requiredScope))
                    .toList();
            }

            return List.of();
        }

        return REQUIRED_SPOTIFY_PLAYBACK_SCOPES.stream()
            .filter(requiredScope -> !scopes.contains(requiredScope))
            .toList();
    }

    private String playbackClientId(String platformId) {
        if ("spotify".equals(platformId)) {
            return blankToNull(platformOAuthProperties.getSpotify().getClientId());
        }
        if ("tidal".equals(platformId)) {
            String clientId = blankToNull(platformOAuthProperties.getTidal().getClientId());
            if (clientId == null) {
                throw new IllegalArgumentException(
                    "TIDAL playback client id is not configured. Set TIDAL_CLIENT_ID before starting TIDAL playback."
                );
            }
            return clientId;
        }

        return null;
    }

    private String playbackCountryCode(PlatformAccountCredential credential) {
        if (!"tidal".equals(credential.platformId())) {
            return null;
        }

        return firstNonBlank(
            countryCodeFromAccessToken(credential.accessToken()),
            platformOAuthProperties.getTidal().getCountryCode()
        );
    }

    private String countryCodeFromAccessToken(String accessToken) {
        return claimFromAccessToken(accessToken, "cc");
    }

    private String claimFromAccessToken(String accessToken, String key) {
        if (accessToken == null || accessToken.isBlank()) {
            return null;
        }

        String[] jwtParts = accessToken.split("\\.");
        if (jwtParts.length < 2) {
            return null;
        }

        try {
            String payload = new String(Base64.getUrlDecoder().decode(jwtParts[1]), StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = objectMapper.readValue(payload, Map.class);
            Object value = claims.get(key);
            return value == null ? null : blankToNull(value.toString());
        } catch (RuntimeException | java.io.IOException exception) {
            return null;
        }
    }

    private boolean requiresProviderPlaybackUserId(String platformId) {
        return "tidal".equals(platformId);
    }

    private boolean isLocalFallbackExternalUserId(String platformId, String externalUserId, String userId) {
        if (externalUserId == null || externalUserId.isBlank()) {
            return true;
        }

        String trimmedExternalUserId = externalUserId.trim();
        String syntheticExternalUserId = "%s:%s".formatted(platformId.replace('-', '_'), userId);
        return trimmedExternalUserId.equals(userId) || trimmedExternalUserId.equals(syntheticExternalUserId);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
