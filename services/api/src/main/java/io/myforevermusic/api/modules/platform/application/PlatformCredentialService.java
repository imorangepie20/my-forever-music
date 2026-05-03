package io.myforevermusic.api.modules.platform.application;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PlatformCredentialService {

    private static final Logger log = LoggerFactory.getLogger(PlatformCredentialService.class);
    private static final Duration REFRESH_BUFFER = Duration.ofSeconds(60);

    private final PlatformCredentialStore platformCredentialStore;
    private final PlatformTokenRefreshRegistry platformTokenRefreshRegistry;

    public PlatformCredentialService(
        PlatformCredentialStore platformCredentialStore,
        PlatformTokenRefreshRegistry platformTokenRefreshRegistry
    ) {
        this.platformCredentialStore = platformCredentialStore;
        this.platformTokenRefreshRegistry = platformTokenRefreshRegistry;
    }

    public Optional<PlatformAccountCredential> findUsableCredential(String userId, String platformId) {
        return resolveCredential(userId, platformId).usableCredential();
    }

    public PlatformCredentialResolution resolveCredential(String userId, String platformId) {
        return platformCredentialStore.findByUserIdAndPlatformId(userId, platformId)
            .map(this::resolveCredential)
            .orElseGet(PlatformCredentialResolution::missing);
    }

    public PlatformCredentialResolution resolveCredential(PlatformAccountCredential credential) {
        Instant now = Instant.now();
        if (!credential.isExpiringWithin(REFRESH_BUFFER, now)) {
            return PlatformCredentialResolution.ready(credential);
        }
        if (credential.refreshToken() == null || credential.refreshToken().isBlank()) {
            return PlatformCredentialResolution.reconnectRequired(
                "Stored platform credential expired and no refresh token is available."
            );
        }
        if (!platformTokenRefreshRegistry.supports(credential)) {
            return PlatformCredentialResolution.reconnectRequired(
                "Stored platform credential expired and automatic refresh is not supported."
            );
        }

        try {
            PlatformTokenExchangeResult refreshResult = platformTokenRefreshRegistry
                .getRequiredClient(credential)
                .refreshAccessToken(credential);
            return PlatformCredentialResolution.ready(saveRefreshedCredential(credential, refreshResult, now));
        } catch (RuntimeException exception) {
            log.warn(
                "Platform credential refresh failed for user {} and platform {}: {}",
                credential.userId(),
                credential.platformId(),
                exception.getMessage()
            );
            return PlatformCredentialResolution.reconnectRequired(
                "Stored platform credential could not be refreshed automatically."
            );
        }
    }

    private PlatformAccountCredential saveRefreshedCredential(
        PlatformAccountCredential currentCredential,
        PlatformTokenExchangeResult refreshResult,
        Instant refreshedAt
    ) {
        List<String> grantedScopes = refreshResult.grantedScopes() == null || refreshResult.grantedScopes().isEmpty()
            ? parseScopeSummary(currentCredential.scopeSummary())
            : refreshResult.grantedScopes();
        String nextRefreshToken = refreshResult.refreshToken() == null || refreshResult.refreshToken().isBlank()
            ? currentCredential.refreshToken()
            : refreshResult.refreshToken();

        PlatformAccountCredential refreshedCredential = new PlatformAccountCredential(
            currentCredential.userId(),
            currentCredential.platformId(),
            currentCredential.authorizationMode(),
            currentCredential.externalUserId(),
            currentCredential.externalAccountLabel(),
            refreshResult.accessToken(),
            nextRefreshToken,
            refreshResult.tokenType() == null || refreshResult.tokenType().isBlank()
                ? currentCredential.tokenType()
                : refreshResult.tokenType(),
            grantedScopes.isEmpty() ? currentCredential.scopeSummary() : String.join(", ", grantedScopes),
            refreshResult.accessTokenExpiresAt(),
            currentCredential.issuedAt(),
            refreshedAt
        );

        return platformCredentialStore.save(refreshedCredential);
    }

    private List<String> parseScopeSummary(String scopeSummary) {
        if (scopeSummary == null || scopeSummary.isBlank()) {
            return List.of();
        }

        return Arrays.stream(scopeSummary.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .toList();
    }
}
