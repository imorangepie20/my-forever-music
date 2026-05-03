package io.myforevermusic.api.modules.pms.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.myforevermusic.api.modules.auth.application.AuthRegistrationService;
import io.myforevermusic.api.modules.auth.infrastructure.local.InMemoryAuthAccountStore;
import io.myforevermusic.api.modules.auth.presentation.AuthRegistrationRequest;
import io.myforevermusic.api.modules.platform.application.PlatformCatalogService;
import io.myforevermusic.api.modules.platform.application.PlatformConnectionService;
import io.myforevermusic.api.modules.platform.application.PlatformPlaylistProviderRegistry;
import io.myforevermusic.api.modules.platform.infrastructure.local.InMemoryPlatformCredentialStore;
import io.myforevermusic.api.modules.platform.infrastructure.local.InMemoryPlatformConnectionStore;
import io.myforevermusic.api.modules.platform.infrastructure.sandbox.SandboxPlatformPlaylistProvider;
import io.myforevermusic.api.modules.platform.presentation.PlatformConnectRequest;
import io.myforevermusic.api.modules.pms.infrastructure.local.InMemoryPmsPlaylistImportStore;
import io.myforevermusic.api.modules.pms.presentation.PmsWorkspaceBootstrapResponse;
import io.myforevermusic.api.modules.pms.presentation.PmsPlaylistImportRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PmsPlaylistImportServiceTest {

    @Test
    void shouldImportSandboxPlatformPlaylistsAndExposeWorkspaceBootstrap() {
        InMemoryAuthAccountStore authAccountStore = new InMemoryAuthAccountStore();
        InMemoryPlatformConnectionStore platformConnectionStore = new InMemoryPlatformConnectionStore();
        InMemoryPlatformCredentialStore platformCredentialStore = new InMemoryPlatformCredentialStore();
        AuthRegistrationService authRegistrationService = new AuthRegistrationService(
            authAccountStore,
            new BCryptPasswordEncoder()
        );
        String userId = authRegistrationService.register(new AuthRegistrationRequest(
            "Forever Listener",
            "listener@example.com",
            "music2026",
            "spotify",
            false,
            true,
            true
        )).user().userId();

        PlatformConnectionService connectionService = new PlatformConnectionService(
            authAccountStore,
            new PlatformCatalogService(),
            platformConnectionStore,
            platformCredentialStore
        );
        connectionService.connect(new PlatformConnectRequest(userId, "spotify", "sandbox-oauth", null));

        InMemoryPmsPlaylistImportStore importStore = new InMemoryPmsPlaylistImportStore();
        PmsPlaylistImportService importService = new PmsPlaylistImportService(
            authAccountStore,
            new PlatformCatalogService(),
            platformConnectionStore,
            platformCredentialStore,
            new PlatformPlaylistProviderRegistry(
                List.of(new SandboxPlatformPlaylistProvider(new PmsPlaylistImportCatalogService()))
            ),
            importStore
        );

        var bootstrapBeforeImport = importService.getBootstrap(userId);
        var importResponse = importService.importPlaylists(
            new PmsPlaylistImportRequest(
                userId,
                "spotify",
                List.of("spotify-liked-night-drive", "spotify-chill-focus-stack")
            )
        );
        var workspaceBootstrap = new PmsImportedWorkspaceBootstrapSource(importStore).load(userId).orElseThrow();

        assertThat(bootstrapBeforeImport.summary().preferredPlatformConnected()).isTrue();
        assertThat(bootstrapBeforeImport.availablePlaylists()).hasSize(2);
        assertThat(importResponse.status()).isEqualTo("playlists_imported");
        assertThat(importResponse.importResult().importedPlaylistCount()).isEqualTo(2);
        assertThat(importResponse.importResult().completeSpotifyAudioFeatureTrackCount()).isEqualTo(6);
        assertThat(importResponse.nextStep().path()).isEqualTo("/ems");
        assertThat(workspaceBootstrap.playlists()).hasSize(2);
        assertThat(workspaceBootstrap.workspaceDefaults().userId()).isEqualTo(userId);
        assertThat(workspaceBootstrap.suggestedTracks()).allMatch(
            PmsWorkspaceBootstrapResponse.TrackSeedSuggestion::spotifyAudioFeaturesFilled
        );
    }
}
