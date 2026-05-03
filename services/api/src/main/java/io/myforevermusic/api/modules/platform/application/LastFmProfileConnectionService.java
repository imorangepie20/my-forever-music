package io.myforevermusic.api.modules.platform.application;

import io.myforevermusic.api.common.error.ApiResourceNotFoundException;
import io.myforevermusic.api.modules.auth.application.AuthAccountStore;
import io.myforevermusic.api.modules.auth.application.AuthRegisteredAccount;
import io.myforevermusic.api.modules.platform.infrastructure.lastfm.LastFmWebApiClient;
import io.myforevermusic.api.modules.platform.presentation.LastFmProfileConnectRequest;
import io.myforevermusic.api.modules.platform.presentation.PlatformCatalogResponse.PlatformOption;
import io.myforevermusic.api.modules.platform.presentation.PlatformConnectionCommandResponse;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class LastFmProfileConnectionService {

    private final AuthAccountStore authAccountStore;
    private final PlatformCatalogService platformCatalogService;
    private final PlatformConnectionStore platformConnectionStore;
    private final LastFmWebApiClient lastFmWebApiClient;

    public LastFmProfileConnectionService(
        AuthAccountStore authAccountStore,
        PlatformCatalogService platformCatalogService,
        PlatformConnectionStore platformConnectionStore,
        LastFmWebApiClient lastFmWebApiClient
    ) {
        this.authAccountStore = authAccountStore;
        this.platformCatalogService = platformCatalogService;
        this.platformConnectionStore = platformConnectionStore;
        this.lastFmWebApiClient = lastFmWebApiClient;
    }

    public PlatformConnectionCommandResponse connectProfile(LastFmProfileConnectRequest request) {
        AuthRegisteredAccount account = authAccountStore.findByUserId(request.userId())
            .orElseThrow(() -> new ApiResourceNotFoundException("No registered account found for user: %s".formatted(request.userId())));
        PlatformOption platform = platformCatalogService.getRequiredPlatform("last-fm");
        String username = normalizeUsername(request.username());
        LastFmWebApiClient.LastFmUserProfile profile = lastFmWebApiClient.getUserProfile(username);
        Instant now = Instant.now();

        authAccountStore.saveLastFmProfile(account.userId(), profile.username(), now);
        PlatformConnectionState state = platformConnectionStore.connect(
            new PlatformConnectionDraft(
                account.userId(),
                "last-fm",
                "public-profile",
                profile.username(),
                "recent-scrobbles-read, top-artists-read, top-tracks-read",
                false,
                now,
                now
            )
        );

        return new PlatformConnectionCommandResponse(
            "api",
            "connected",
            now,
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
                "/platforms",
                "Last.fm signal profile saved. You can use it for EMS analysis or choose another PMS import source."
            )
        );
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Last.fm username is required.");
        }
        return username.trim();
    }
}
