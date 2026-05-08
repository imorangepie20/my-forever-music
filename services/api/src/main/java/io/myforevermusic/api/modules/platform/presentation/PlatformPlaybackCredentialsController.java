package io.myforevermusic.api.modules.platform.presentation;

import io.myforevermusic.api.modules.platform.application.PlatformAccountCredential;
import io.myforevermusic.api.modules.platform.application.PlatformCredentialResolution;
import io.myforevermusic.api.modules.platform.application.PlatformCredentialService;
import io.myforevermusic.api.modules.platform.application.PlatformReconnectRequiredException;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
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

    private final PlatformCredentialService platformCredentialService;

    public PlatformPlaybackCredentialsController(PlatformCredentialService platformCredentialService) {
        this.platformCredentialService = platformCredentialService;
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
            credential.externalAccountLabel(),
            credential.authorizationMode()
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
            return List.of();
        }

        return REQUIRED_SPOTIFY_PLAYBACK_SCOPES.stream()
            .filter(requiredScope -> !scopes.contains(requiredScope))
            .toList();
    }
}
