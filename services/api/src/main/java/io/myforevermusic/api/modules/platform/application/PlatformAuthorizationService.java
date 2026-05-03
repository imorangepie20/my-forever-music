package io.myforevermusic.api.modules.platform.application;

import io.myforevermusic.api.common.error.ApiResourceNotFoundException;
import io.myforevermusic.api.modules.auth.application.AuthAccountStore;
import io.myforevermusic.api.modules.auth.application.AuthRegisteredAccount;
import io.myforevermusic.api.modules.platform.presentation.PlatformAuthorizationCompleteRequest;
import io.myforevermusic.api.modules.platform.presentation.PlatformAuthorizationCompleteResponse;
import io.myforevermusic.api.modules.platform.presentation.PlatformAuthorizationStartRequest;
import io.myforevermusic.api.modules.platform.presentation.PlatformAuthorizationStartResponse;
import io.myforevermusic.api.modules.platform.presentation.PlatformCatalogResponse.PlatformOption;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import org.springframework.stereotype.Service;

@Service
public class PlatformAuthorizationService {

    private static final List<String> DEFAULT_REQUESTED_SCOPES = List.of("playlist-read", "profile-read");
    private static final String SANDBOX_APPROVAL_CODE = "sandbox-approved";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AuthAccountStore authAccountStore;
    private final PlatformCatalogService platformCatalogService;
    private final PlatformAuthorizationSessionStore platformAuthorizationSessionStore;
    private final PlatformConnectionStore platformConnectionStore;
    private final PlatformCredentialStore platformCredentialStore;
    private final PlatformAuthorizationCodeExchangeRegistry platformAuthorizationCodeExchangeRegistry;
    private final PlatformOAuthProperties platformOAuthProperties;

    public PlatformAuthorizationService(
        AuthAccountStore authAccountStore,
        PlatformCatalogService platformCatalogService,
        PlatformAuthorizationSessionStore platformAuthorizationSessionStore,
        PlatformConnectionStore platformConnectionStore,
        PlatformCredentialStore platformCredentialStore,
        PlatformAuthorizationCodeExchangeRegistry platformAuthorizationCodeExchangeRegistry,
        PlatformOAuthProperties platformOAuthProperties
    ) {
        this.authAccountStore = authAccountStore;
        this.platformCatalogService = platformCatalogService;
        this.platformAuthorizationSessionStore = platformAuthorizationSessionStore;
        this.platformConnectionStore = platformConnectionStore;
        this.platformCredentialStore = platformCredentialStore;
        this.platformAuthorizationCodeExchangeRegistry = platformAuthorizationCodeExchangeRegistry;
        this.platformOAuthProperties = platformOAuthProperties;
    }

    public PlatformAuthorizationStartResponse startAuthorization(PlatformAuthorizationStartRequest request) {
        AuthRegisteredAccount account = findAccount(request.userId());
        PlatformOption platform = findPlatform(request.platformId());
        Instant now = Instant.now();
        String state = "oauth-%s".formatted(UUID.randomUUID());
        boolean spotifyPkceEnabled = "spotify".equals(platform.platformId())
            && platformOAuthProperties.getSpotify().isConfigured();
        String authorizationMode = spotifyPkceEnabled ? "spotify-pkce-draft" : "sandbox-oauth";
        String authorizationChannel = spotifyPkceEnabled ? "external_browser_redirect" : "internal_approval_page";
        List<String> requestedScopes = spotifyPkceEnabled
            ? platformOAuthProperties.getSpotify().getScopes()
            : DEFAULT_REQUESTED_SCOPES;
        String pkceCodeVerifier = spotifyPkceEnabled ? generatePkceCodeVerifier() : null;
        String externalAuthorizationUrl = spotifyPkceEnabled
            ? buildSpotifyAuthorizationUrl(state, requestedScopes, pkceCodeVerifier)
            : null;
        String redirectUri = spotifyPkceEnabled ? platformOAuthProperties.getSpotify().getRedirectUri() : null;
        String approvalCode = spotifyPkceEnabled ? null : SANDBOX_APPROVAL_CODE;

        PlatformAuthorizationSession session = platformAuthorizationSessionStore.create(
            new PlatformAuthorizationSessionDraft(
                state,
                account.userId(),
                platform.platformId(),
                platform.displayName(),
                authorizationMode,
                authorizationChannel,
                requestedScopes,
                approvalCode,
                externalAuthorizationUrl,
                redirectUri,
                pkceCodeVerifier,
                now.plusSeconds(600),
                now
            )
        );

        return new PlatformAuthorizationStartResponse(
            "api",
            "authorization_pending",
            now,
            new PlatformAuthorizationStartResponse.AuthorizationUser(
                account.userId(),
                account.displayName(),
                account.email()
            ),
            new PlatformAuthorizationStartResponse.AuthorizationSession(
                session.state(),
                session.platformId(),
                session.platformDisplayName(),
                session.authorizationMode(),
                session.authorizationChannel(),
                session.requestedScopes(),
                session.expiresAt(),
                "internal_approval_page".equals(session.authorizationChannel())
                    ? "/platforms/oauth/authorize?state=%s".formatted(session.state())
                    : null,
                "internal_approval_page".equals(session.authorizationChannel())
                    ? "/platforms/oauth/callback?state=%s&code=%s".formatted(session.state(), session.approvalCode())
                    : "%s?state=%s".formatted(session.redirectUri(), session.state()),
                session.approvalCode(),
                session.externalAuthorizationUrl(),
                session.redirectUri()
            )
        );
    }

    public PlatformAuthorizationCompleteResponse completeAuthorization(PlatformAuthorizationCompleteRequest request) {
        AuthRegisteredAccount account = findAccount(request.userId());
        PlatformAuthorizationSession session = platformAuthorizationSessionStore.findByState(request.state())
            .orElseThrow(() -> new ApiResourceNotFoundException("No pending platform authorization was found for state: %s".formatted(request.state())));

        if (!session.userId().equals(request.userId()) || !session.platformId().equals(request.platformId())) {
            throw new IllegalArgumentException("Authorization session does not match the current user or platform.");
        }
        if (session.isCompleted()) {
            throw new IllegalArgumentException("Authorization session has already been completed.");
        }
        if (session.isExpired(Instant.now())) {
            throw new IllegalArgumentException("Authorization session has expired. Start the platform connection again.");
        }
        if ("internal_approval_page".equals(session.authorizationChannel())) {
            if (request.approvalCode() == null || !session.approvalCode().equals(request.approvalCode())) {
                throw new IllegalArgumentException("Authorization approval code is invalid.");
            }
        } else if (request.authorizationCode() == null || request.authorizationCode().isBlank()) {
            throw new IllegalArgumentException("Provider authorization code is required.");
        }

        Instant now = Instant.now();
        String callbackCode = "internal_approval_page".equals(session.authorizationChannel())
            ? request.approvalCode()
            : request.authorizationCode();
        PlatformTokenExchangeResult tokenExchangeResult = platformAuthorizationCodeExchangeRegistry
            .getRequiredClient(session)
            .exchangeAuthorizationCode(session, callbackCode);
        PlatformOption platform = findPlatform(session.platformId());
        String scopeSummary = String.join(", ", tokenExchangeResult.grantedScopes());
        String externalAccountLabel = "%s %s account".formatted(account.displayName(), session.platformDisplayName());

        platformCredentialStore.save(
            new PlatformAccountCredential(
                account.userId(),
                session.platformId(),
                session.authorizationMode(),
                "%s:%s".formatted(session.platformId().replace('-', '_'), account.userId()),
                externalAccountLabel,
                tokenExchangeResult.accessToken(),
                tokenExchangeResult.refreshToken(),
                tokenExchangeResult.tokenType(),
                scopeSummary,
                tokenExchangeResult.accessTokenExpiresAt(),
                now,
                now
            )
        );

        PlatformConnectionState connectionState = platformConnectionStore.connect(
            new PlatformConnectionDraft(
                account.userId(),
                session.platformId(),
                session.authorizationMode(),
                externalAccountLabel,
                scopeSummary,
                platform.pmsImportSupported(),
                now,
                now
            )
        );
        PlatformAuthorizationSession completedSession = platformAuthorizationSessionStore.markCompleted(session.state(), now);

        boolean preferredConnected = account.preferredPlatformId().equals(session.platformId())
            && connectionState.connected()
            && platform.pmsImportSupported();

        return new PlatformAuthorizationCompleteResponse(
            "api",
            "authorization_completed",
            now,
            new PlatformAuthorizationCompleteResponse.AuthorizationResult(
                completedSession.state(),
                completedSession.platformId(),
                completedSession.platformDisplayName(),
                completedSession.authorizationMode(),
                completedSession.requestedScopes(),
                completedSession.completedAt()
            ),
            new PlatformAuthorizationCompleteResponse.ConnectionResult(
                account.userId(),
                connectionState.platformId(),
                connectionState.connected(),
                connectionState.connectionStatus(),
                connectionState.connectionMode(),
                connectionState.externalAccountLabel(),
                connectionState.scopeSummary(),
                connectionState.syncReady(),
                connectionState.connectedAt()
            ),
            new PlatformAuthorizationCompleteResponse.NextStep(
                preferredConnected ? "/pms" : "/platforms",
                preferredConnected
                    ? "Preferred platform connected. Continue to PMS import."
                    : account.preferredPlatformId().equals(session.platformId()) && !platform.pmsImportSupported()
                        ? "Platform connected. This source is available for analysis signals, but PMS import is not ready yet."
                        : "Platform connected. Return to platform onboarding to continue setup."
            )
        );
    }

    private AuthRegisteredAccount findAccount(String userId) {
        return authAccountStore.findByUserId(userId)
            .orElseThrow(() -> new ApiResourceNotFoundException("No registered account found for user: %s".formatted(userId)));
    }

    private PlatformOption findPlatform(String platformId) {
        return platformCatalogService.getCatalog()
            .platforms()
            .stream()
            .filter(platform -> platform.platformId().equals(platformId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Platform is not supported: %s".formatted(platformId)));
    }

    private String buildSpotifyAuthorizationUrl(
        String state,
        List<String> requestedScopes,
        String pkceCodeVerifier
    ) {
        String codeChallenge = toCodeChallenge(pkceCodeVerifier);
        PlatformOAuthProperties.Spotify spotify = platformOAuthProperties.getSpotify();

        return spotify.getAuthorizationUri()
            + "?client_id=" + encode(spotify.getClientId())
            + "&response_type=code"
            + "&redirect_uri=" + encode(spotify.getRedirectUri())
            + "&scope=" + encode(String.join(" ", requestedScopes))
            + "&state=" + encode(state)
            + "&code_challenge_method=S256"
            + "&code_challenge=" + encode(codeChallenge);
    }

    private String generatePkceCodeVerifier() {
        byte[] random = new byte[64];
        SECURE_RANDOM.nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    private String toCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required for PKCE support.", exception);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
