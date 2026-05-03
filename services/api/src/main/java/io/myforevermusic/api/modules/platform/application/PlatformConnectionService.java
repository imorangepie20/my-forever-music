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
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;

@Service
public class PlatformConnectionService {

    private final AuthAccountStore authAccountStore;
    private final PlatformCatalogService platformCatalogService;
    private final PlatformConnectionStore platformConnectionStore;
    private final PlatformCredentialStore platformCredentialStore;

    public PlatformConnectionService(
        AuthAccountStore authAccountStore,
        PlatformCatalogService platformCatalogService,
        PlatformConnectionStore platformConnectionStore,
        PlatformCredentialStore platformCredentialStore
    ) {
        this.authAccountStore = authAccountStore;
        this.platformCatalogService = platformCatalogService;
        this.platformConnectionStore = platformConnectionStore;
        this.platformCredentialStore = platformCredentialStore;
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

        boolean preferredConnected = connections.stream()
            .anyMatch(connection -> connection.preferred() && connection.connected() && connection.syncReady());

        return new PlatformConnectionBootstrapResponse(
            "api",
            "ok",
            Instant.now(),
            new PlatformConnectionBootstrapResponse.ConnectionUser(
                account.userId(),
                account.displayName(),
                account.email(),
                account.preferredPlatformId()
            ),
            new PlatformConnectionBootstrapResponse.ConnectionSummary(
                (int) connections.stream().filter(PlatformConnectionBootstrapResponse.PlatformConnectionCard::connected).count(),
                preferredConnected,
                preferredConnected ? "import-playlists" : "connect-platform",
                preferredConnected ? "/pms" : "/platforms",
                preferredConnected
                    ? "Preferred platform is connected. You can continue into PMS import."
                    : "Connect your preferred platform first, then continue to PMS import."
            ),
            connections
        );
    }

    public PlatformConnectionCommandResponse connect(PlatformConnectRequest request) {
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
                true,
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
        Optional<PlatformAccountCredential> credential = platformCredentialStore.findByUserIdAndPlatformId(
            account.userId(),
            platform.platformId()
        );
        boolean connected = state != null && state.connected();
        boolean syncReady = connected && credential.isPresent() && !credential.orElseThrow().isExpired(Instant.now());

        return new PlatformConnectionBootstrapResponse.PlatformConnectionCard(
            platform.platformId(),
            platform.displayName(),
            preferred,
            connected,
            state == null ? "not_connected" : state.connectionStatus(),
            state == null ? null : state.connectionMode(),
            state == null ? null : state.externalAccountLabel(),
            syncReady,
            state == null ? null : state.connectedAt(),
            connected ? "Disconnect" : "Connect"
        );
    }

    private PlatformConnectionCommandResponse toCommandResponse(
        AuthRegisteredAccount account,
        PlatformOption platform,
        PlatformConnectionState state,
        String status
    ) {
        boolean preferredConnected = state.connected() && account.preferredPlatformId().equals(platform.platformId());

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
