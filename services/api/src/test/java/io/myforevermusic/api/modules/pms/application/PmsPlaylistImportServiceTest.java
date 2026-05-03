package io.myforevermusic.api.modules.pms.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.myforevermusic.api.modules.auth.application.AuthRegistrationService;
import io.myforevermusic.api.modules.auth.infrastructure.local.InMemoryAuthAccountStore;
import io.myforevermusic.api.modules.auth.presentation.AuthRegistrationRequest;
import io.myforevermusic.api.modules.platform.application.PlatformAccountCredential;
import io.myforevermusic.api.modules.platform.application.PlatformCatalogService;
import io.myforevermusic.api.modules.platform.application.PlatformCredentialService;
import io.myforevermusic.api.modules.platform.application.PlatformConnectionService;
import io.myforevermusic.api.modules.platform.application.PlatformPlaylistProvider;
import io.myforevermusic.api.modules.platform.application.PlatformPlaylistProviderRegistry;
import io.myforevermusic.api.modules.platform.application.PlatformReconnectRequiredException;
import io.myforevermusic.api.modules.platform.application.PlatformTokenExchangeResult;
import io.myforevermusic.api.modules.platform.application.PlatformTokenRefreshClient;
import io.myforevermusic.api.modules.platform.application.PlatformTokenRefreshRegistry;
import io.myforevermusic.api.modules.platform.infrastructure.local.InMemoryPlatformCredentialStore;
import io.myforevermusic.api.modules.platform.infrastructure.local.InMemoryPlatformConnectionStore;
import io.myforevermusic.api.modules.platform.infrastructure.sandbox.SandboxPlatformPlaylistProvider;
import io.myforevermusic.api.modules.platform.presentation.PlatformConnectRequest;
import io.myforevermusic.api.modules.pms.application.PmsPlaylistImportCatalogService.ImportCandidatePlaylist;
import io.myforevermusic.api.modules.pms.application.PmsPlaylistImportCatalogService.ImportCandidateTrack;
import io.myforevermusic.api.modules.pms.infrastructure.persistence.PmsTrackSpotifyAudioFeatures;
import io.myforevermusic.api.modules.pms.infrastructure.local.InMemoryPmsPlaylistImportStore;
import io.myforevermusic.api.modules.pms.presentation.PmsPlaylistImportRequest;
import io.myforevermusic.api.modules.pms.presentation.PmsWorkspaceBootstrapResponse;
import java.time.Instant;
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
            platformCredentialStore,
            new PlatformCredentialService(
                platformCredentialStore,
                new PlatformTokenRefreshRegistry(List.of())
            )
        );
        connectionService.connect(new PlatformConnectRequest(userId, "spotify", "sandbox-oauth", null));

        InMemoryPmsPlaylistImportStore importStore = new InMemoryPmsPlaylistImportStore();
        PmsPlaylistImportService importService = new PmsPlaylistImportService(
            authAccountStore,
            new PlatformCatalogService(),
            platformConnectionStore,
            new PlatformCredentialService(
                platformCredentialStore,
                new PlatformTokenRefreshRegistry(List.of())
            ),
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

    @Test
    void shouldRefreshExpiredSpotifyCredentialBeforeImport() {
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

        platformConnectionStore.connect(new io.myforevermusic.api.modules.platform.application.PlatformConnectionDraft(
            userId,
            "spotify",
            "spotify-pkce-draft",
            "Forever Listener Spotify",
            "playlist-read-private, user-read-email",
            true,
            Instant.parse("2026-05-03T00:00:00Z"),
            Instant.parse("2026-05-03T00:00:00Z")
        ));
        platformCredentialStore.save(new PlatformAccountCredential(
            userId,
            "spotify",
            "spotify-pkce-draft",
            "spotify-user-001",
            "Forever Listener Spotify",
            "expired-access-token",
            "spotify-refresh-token",
            "Bearer",
            "playlist-read-private, user-read-email",
            Instant.now().minusSeconds(30),
            Instant.parse("2026-05-03T00:00:00Z"),
            Instant.parse("2026-05-03T00:00:00Z")
        ));

        PlatformTokenRefreshClient refreshClient = new PlatformTokenRefreshClient() {
            @Override
            public boolean supports(PlatformAccountCredential credential) {
                return "spotify".equals(credential.platformId());
            }

            @Override
            public PlatformTokenExchangeResult refreshAccessToken(PlatformAccountCredential credential) {
                return new PlatformTokenExchangeResult(
                    "refreshed-access-token",
                    "",
                    "Bearer",
                    List.of("playlist-read-private", "user-read-email"),
                    Instant.now().plusSeconds(3600)
                );
            }
        };

        PlatformCredentialService platformCredentialService = new PlatformCredentialService(
            platformCredentialStore,
            new PlatformTokenRefreshRegistry(List.of(refreshClient))
        );
        InMemoryPmsPlaylistImportStore importStore = new InMemoryPmsPlaylistImportStore();
        PmsPlaylistImportService importService = new PmsPlaylistImportService(
            authAccountStore,
            new PlatformCatalogService(),
            platformConnectionStore,
            platformCredentialService,
            new PlatformPlaylistProviderRegistry(List.of(new PlatformPlaylistProvider() {
                @Override
                public boolean supports(String platformId, PlatformAccountCredential credential) {
                    return "spotify".equals(platformId)
                        && "refreshed-access-token".equals(credential.accessToken());
                }

                @Override
                public List<ImportCandidatePlaylist> listImportablePlaylists(
                    io.myforevermusic.api.modules.auth.application.AuthRegisteredAccount account,
                    PlatformAccountCredential credential
                ) {
                    return List.of(
                        new ImportCandidatePlaylist(
                            "spotify-owned-001",
                            "Refreshed Playlist",
                            "spotify",
                            "Forever Listener",
                            "Loaded after refresh.",
                            1,
                            List.of()
                        )
                    );
                }

                @Override
                public List<ImportCandidatePlaylist> loadPlaylistsForImport(
                    io.myforevermusic.api.modules.auth.application.AuthRegisteredAccount account,
                    PlatformAccountCredential credential,
                    List<String> externalPlaylistIds
                ) {
                    return List.of(
                        new ImportCandidatePlaylist(
                            "spotify-owned-001",
                            "Refreshed Playlist",
                            "spotify",
                            "Forever Listener",
                            "Loaded after refresh.",
                            1,
                            List.of(
                                new ImportCandidateTrack(
                                    "track-001",
                                    "Midnight Receiver",
                                    "Neon Bloom",
                                    "synth-pop",
                                    true,
                                    new PmsTrackSpotifyAudioFeatures(
                                        "track-001",
                                        "spotify_api",
                                        true,
                                        "https://api.spotify.com/v1/audio-analysis/track-001",
                                        "https://api.spotify.com/v1/tracks/track-001",
                                        "spotify:track:track-001",
                                        "audio_features",
                                        218000,
                                        1,
                                        1,
                                        4,
                                        0.19,
                                        0.74,
                                        0.78,
                                        0.02,
                                        0.11,
                                        -7.8,
                                        0.05,
                                        116.2,
                                        0.67,
                                        Instant.now()
                                    )
                                )
                            )
                        )
                    );
                }
            })),
            importStore
        );

        var bootstrap = importService.getBootstrap(userId);
        var importResponse = importService.importPlaylists(new PmsPlaylistImportRequest(
            userId,
            "spotify",
            List.of("spotify-owned-001")
        ));
        var savedCredential = platformCredentialStore.findByUserIdAndPlatformId(userId, "spotify").orElseThrow();

        assertThat(bootstrap.summary().preferredPlatformConnected()).isTrue();
        assertThat(bootstrap.availablePlaylists()).hasSize(1);
        assertThat(importResponse.importResult().importedPlaylistCount()).isEqualTo(1);
        assertThat(savedCredential.accessToken()).isEqualTo("refreshed-access-token");
        assertThat(savedCredential.refreshToken()).isEqualTo("spotify-refresh-token");
    }

    @Test
    void shouldRequireReconnectWhenConnectedPlatformCredentialIsNoLongerUsable() {
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
            platformCredentialStore,
            new PlatformCredentialService(
                platformCredentialStore,
                new PlatformTokenRefreshRegistry(List.of())
            )
        );
        connectionService.connect(new PlatformConnectRequest(userId, "spotify", "spotify-pkce-draft", null));
        platformCredentialStore.save(new PlatformAccountCredential(
            userId,
            "spotify",
            "spotify-pkce-draft",
            "spotify-user-001",
            "Forever Listener Spotify",
            "expired-access-token",
            "",
            "Bearer",
            "playlist-read-private",
            Instant.now().minusSeconds(30),
            Instant.parse("2026-05-03T00:00:00Z"),
            Instant.parse("2026-05-03T00:00:00Z")
        ));

        InMemoryPmsPlaylistImportStore importStore = new InMemoryPmsPlaylistImportStore();
        PmsPlaylistImportService importService = new PmsPlaylistImportService(
            authAccountStore,
            new PlatformCatalogService(),
            platformConnectionStore,
            new PlatformCredentialService(
                platformCredentialStore,
                new PlatformTokenRefreshRegistry(List.of())
            ),
            new PlatformPlaylistProviderRegistry(
                List.of(new SandboxPlatformPlaylistProvider(new PmsPlaylistImportCatalogService()))
            ),
            importStore
        );

        var bootstrap = importService.getBootstrap(userId);

        assertThat(bootstrap.platformConnection().connected()).isTrue();
        assertThat(bootstrap.platformConnection().syncReady()).isFalse();
        assertThat(bootstrap.platformConnection().reconnectRequired()).isTrue();
        assertThat(bootstrap.summary().reconnectRequired()).isTrue();

        assertThatThrownBy(() -> importService.importPlaylists(new PmsPlaylistImportRequest(
            userId,
            "spotify",
            List.of("spotify-liked-night-drive")
        )))
            .isInstanceOf(PlatformReconnectRequiredException.class)
            .hasMessageContaining("Reconnect Spotify");
    }
}
