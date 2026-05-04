package io.myforevermusic.api.modules.platform.application;

import io.myforevermusic.api.common.error.ApiResourceNotFoundException;
import io.myforevermusic.api.modules.auth.application.AuthAccountStore;
import io.myforevermusic.api.modules.auth.application.AuthRegisteredAccount;
import io.myforevermusic.api.modules.platform.presentation.PlatformConnectionBootstrapResponse;
import io.myforevermusic.api.modules.platform.presentation.PlatformConnectionCommandResponse;
import io.myforevermusic.api.modules.platform.presentation.PlatformConnectRequest;
import io.myforevermusic.api.modules.platform.presentation.PlatformDisconnectRequest;
import io.myforevermusic.api.modules.platform.presentation.PlatformCatalogResponse.PlatformOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;

@Service
public class PlatformConnectionService {

    private final AuthAccountStore authAccountStore;
    private final PlatformCatalogService platformCatalogService;
    private final PlatformConnectionStore platformConnectionStore;
    private final PlatformCredentialStore platformCredentialStore;
    private final PlatformCredentialService platformCredentialService;

    public PlatformConnectionService(
        AuthAccountStore authAccountStore,
        PlatformCatalogService platformCatalogService,
        PlatformConnectionStore platformConnectionStore,
        PlatformCredentialStore platformCredentialStore,
        PlatformCredentialService platformCredentialService
    ) {
        this.authAccountStore = authAccountStore;
        this.platformCatalogService = platformCatalogService;
        this.platformConnectionStore = platformConnectionStore;
        this.platformCredentialStore = platformCredentialStore;
        this.platformCredentialService = platformCredentialService;
    }

    public PlatformConnectionBootstrapResponse getBootstrap(String userId) {
        AuthRegisteredAccount account = findAccount(userId);
        Map<String, PlatformConnectionState> statesByPlatformId = platformConnectionStore.findByUserId(userId)
            .stream()
            .collect(java.util.stream.Collectors.toMap(PlatformConnectionState::platformId, Function.identity()));

        List<PlatformOption> catalog = platformCatalogService.getCatalog().platforms();
        List<PlatformConnectionBootstrapResponse.PlatformConnectionCard> connections = catalog.stream()
            .map(platform -> toCard(platform, account, statesByPlatformId.get(platform.platformId())))
            .sorted(Comparator.comparing(PlatformConnectionBootstrapResponse.PlatformConnectionCard::preferred).reversed()
                .thenComparing(PlatformConnectionBootstrapResponse.PlatformConnectionCard::displayName))
            .toList();

        PlatformConnectionBootstrapResponse.PlatformConnectionCard preferredConnection = connections.stream()
            .filter(PlatformConnectionBootstrapResponse.PlatformConnectionCard::preferred)
            .findFirst()
            .orElse(null);
        PlatformOption preferredPlatform = findPlatform(account.preferredPlatformId());
        boolean preferredConnected = preferredConnection != null
            && preferredConnection.connected()
            && preferredConnection.syncReady();
        boolean preferredReconnectRequired = preferredConnection != null && preferredConnection.reconnectRequired();
        boolean preferredPmsImportUnsupported = preferredConnection != null
            && preferredConnection.connected()
            && !preferredPlatform.pmsImportSupported();

        return new PlatformConnectionBootstrapResponse(
            "api",
            "ok",
            Instant.now(),
            new PlatformConnectionBootstrapResponse.ConnectionUser(
                account.userId(),
                account.displayName(),
                account.email(),
                account.preferredPlatformId(),
                account.lastFmUsername(),
                account.lastFmConnectedAt()
            ),
            new PlatformConnectionBootstrapResponse.ConnectionSummary(
                (int) connections.stream().filter(PlatformConnectionBootstrapResponse.PlatformConnectionCard::connected).count(),
                preferredConnected,
                preferredReconnectRequired,
                preferredConnected ? "import-playlists" : preferredPmsImportUnsupported ? "analysis-only-platform" : "connect-platform",
                preferredConnected ? "/pms" : "/platforms",
                preferredConnected
                    ? "Preferred platform is connected. You can continue into PMS import."
                    : preferredReconnectRequired
                        ? "Preferred platform needs to be reconnected before PMS can import playlists."
                        : preferredPmsImportUnsupported
                            ? "Preferred platform is connected, but PMS playlist import is not supported yet. Choose another PMS source or keep it as an EMS signal platform."
                            : "Connect your preferred platform first, then continue to PMS import."
            ),
            connections
        );
    }

    public PlatformConnectionCommandResponse connect(PlatformConnectRequest request) {
        findAccount(request.userId());
        PlatformOption platform = findPlatform(request.platformId());
        throw new IllegalArgumentException(
            "Direct platform connect is disabled. Use the real OAuth flow or a platform-specific connection endpoint for %s."
                .formatted(platform.displayName())
        );
    }

    public PlatformConnectionCommandResponse connectSandboxForTests(PlatformConnectRequest request) {
        AuthRegisteredAccount account = findAccount(request.userId());
        PlatformOption platform = findPlatform(request.platformId());
        Instant now = Instant.now();
        String connectionMode = request.connectionMode() == null || request.connectionMode().isBlank()
            ? "sandbox"
            : request.connectionMode().trim();
        String externalAccountLabel = request.externalAccountLabel() == null || request.externalAccountLabel().isBlank()
            ? "%s %s account".formatted(account.displayName(), platform.displayName())
            : request.externalAccountLabel().trim();

        PlatformConnectionState state = platformConnectionStore.connect(
            new PlatformConnectionDraft(
                request.userId(),
                request.platformId(),
                connectionMode,
                externalAccountLabel,
                "playlist-read, profile-read",
                platform.pmsImportSupported(),
                now,
                now
            )
        );

        platformCredentialStore.save(
            createCredential(
                account,
                platform.platformId(),
                connectionMode,
                externalAccountLabel,
                "playlist-read, profile-read",
                now
            )
        );

        return toCommandResponse(account, platform, state, "connected");
    }

    public PlatformConnectionCommandResponse disconnect(PlatformDisconnectRequest request) {
        AuthRegisteredAccount account = findAccount(request.userId());
        PlatformOption platform = findPlatform(request.platformId());
        platformCredentialStore.clear(request.userId(), request.platformId());
        if ("last-fm".equals(platform.platformId())) {
            authAccountStore.clearLastFmProfile(request.userId());
        }
        PlatformConnectionState state = platformConnectionStore.disconnect(request.userId(), request.platformId());
        return toCommandResponse(account, platform, state, "disconnected");
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

    private PlatformConnectionBootstrapResponse.PlatformConnectionCard toCard(
        PlatformOption platform,
        AuthRegisteredAccount account,
        PlatformConnectionState state
    ) {
        boolean preferred = account.preferredPlatformId().equals(platform.platformId());
        PlatformCredentialResolution credentialResolution = platformCredentialService.resolveCredential(
            account.userId(),
            platform.platformId()
        );
        boolean connected = state != null && state.connected();
        boolean syncReady = connected && platform.pmsImportSupported() && credentialResolution.usable();
        boolean reconnectRequired = platform.pmsImportSupported() && credentialResolution.needsReconnect(connected);

        return new PlatformConnectionBootstrapResponse.PlatformConnectionCard(
            platform.platformId(),
            platform.displayName(),
            preferred,
            connected,
            state == null ? "not_connected" : state.connectionStatus(),
            state == null ? null : state.connectionMode(),
            state == null ? null : state.externalAccountLabel(),
            syncReady,
            credentialResolution.status(),
            reconnectRequired,
            state == null ? null : state.connectedAt(),
            reconnectRequired ? "Reconnect" : connected ? "Disconnect" : "Connect"
        );
    }

    private PlatformConnectionCommandResponse toCommandResponse(
        AuthRegisteredAccount account,
        PlatformOption platform,
        PlatformConnectionState state,
        String status
    ) {
        boolean preferredConnected = state.connected()
            && account.preferredPlatformId().equals(platform.platformId())
            && platform.pmsImportSupported();

        return new PlatformConnectionCommandResponse(
            "api",
            status,
            Instant.now(),
            new PlatformConnectionCommandResponse.ConnectionResult(
                account.userId(),
                platform.platformId(),
                platform.displayName(),
                state.connected(),
                state.connectionStatus(),
                state.connectionMode(),
                state.externalAccountLabel(),
                state.scopeSummary(),
                state.syncReady(),
                state.connectedAt()
            ),
            new PlatformConnectionCommandResponse.NextStep(
                preferredConnected ? "/pms" : "/platforms",
                preferredConnected
                    ? "Preferred platform connected. Continue to PMS import."
                    : state.connected() && account.preferredPlatformId().equals(platform.platformId()) && !platform.pmsImportSupported()
                        ? "Preferred platform is connected, but PMS import is not available yet for this source."
                        : "You can connect more platforms or continue the onboarding setup."
            )
        );
    }

    private PlatformAccountCredential createCredential(
        AuthRegisteredAccount account,
        String platformId,
        String connectionMode,
        String externalAccountLabel,
        String scopeSummary,
        Instant now
    ) {
        String normalizedPlatformId = platformId.replace('-', '_');
        return new PlatformAccountCredential(
            account.userId(),
            platformId,
            connectionMode,
            "%s:%s".formatted(normalizedPlatformId, account.userId()),
            externalAccountLabel,
            "sandbox-access-%s".formatted(UUID.randomUUID()),
            "sandbox-refresh-%s".formatted(UUID.randomUUID()),
            "Bearer",
            scopeSummary,
            now.plus(1, ChronoUnit.HOURS),
            now,
            now
        );
    }
}
