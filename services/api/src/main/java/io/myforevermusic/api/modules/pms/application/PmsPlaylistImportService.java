package io.myforevermusic.api.modules.pms.application;

import io.myforevermusic.api.common.error.ApiResourceNotFoundException;
import io.myforevermusic.api.modules.auth.application.AuthAccountStore;
import io.myforevermusic.api.modules.auth.application.AuthRegisteredAccount;
import io.myforevermusic.api.modules.platform.application.PlatformAccountCredential;
import io.myforevermusic.api.modules.platform.application.PlatformCatalogService;
import io.myforevermusic.api.modules.platform.application.PlatformConnectionState;
import io.myforevermusic.api.modules.platform.application.PlatformConnectionStore;
import io.myforevermusic.api.modules.platform.application.PlatformCredentialStore;
import io.myforevermusic.api.modules.platform.application.PlatformPlaylistProvider;
import io.myforevermusic.api.modules.platform.application.PlatformPlaylistProviderRegistry;
import io.myforevermusic.api.modules.platform.presentation.PlatformCatalogResponse.PlatformOption;
import io.myforevermusic.api.modules.pms.application.PmsPlaylistImportCatalogService.ImportCandidatePlaylist;
import io.myforevermusic.api.modules.pms.presentation.PmsPlaylistImportBootstrapResponse;
import io.myforevermusic.api.modules.pms.presentation.PmsPlaylistImportRequest;
import io.myforevermusic.api.modules.pms.presentation.PmsPlaylistImportResponse;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class PmsPlaylistImportService {

    private final AuthAccountStore authAccountStore;
    private final PlatformCatalogService platformCatalogService;
    private final PlatformConnectionStore platformConnectionStore;
    private final PlatformCredentialStore platformCredentialStore;
    private final PlatformPlaylistProviderRegistry platformPlaylistProviderRegistry;
    private final PmsPlaylistImportStore pmsPlaylistImportStore;

    public PmsPlaylistImportService(
        AuthAccountStore authAccountStore,
        PlatformCatalogService platformCatalogService,
        PlatformConnectionStore platformConnectionStore,
        PlatformCredentialStore platformCredentialStore,
        PlatformPlaylistProviderRegistry platformPlaylistProviderRegistry,
        PmsPlaylistImportStore pmsPlaylistImportStore
    ) {
        this.authAccountStore = authAccountStore;
        this.platformCatalogService = platformCatalogService;
        this.platformConnectionStore = platformConnectionStore;
        this.platformCredentialStore = platformCredentialStore;
        this.platformPlaylistProviderRegistry = platformPlaylistProviderRegistry;
        this.pmsPlaylistImportStore = pmsPlaylistImportStore;
    }

    public PmsPlaylistImportBootstrapResponse getBootstrap(String userId) {
        AuthRegisteredAccount account = findAccount(userId);
        PlatformOption preferredPlatform = findPlatform(account.preferredPlatformId());
        PlatformConnectionState preferredConnection = findConnection(userId, preferredPlatform.platformId()).orElse(null);
        PlatformAccountCredential preferredCredential = findCredential(userId, preferredPlatform.platformId()).orElse(null);
        boolean preferredCredentialReady = preferredCredential != null && !preferredCredential.isExpired(Instant.now());

        List<ImportCandidatePlaylist> availablePlaylists =
            preferredConnection != null && preferredConnection.connected() && preferredCredentialReady
                ? getProvider(preferredPlatform.platformId(), preferredCredential).listImportablePlaylists(account, preferredCredential)
                : List.of();

        List<PmsPlaylistImportStore.ImportedPlaylistState> importedPlaylists =
            pmsPlaylistImportStore.findImportedPlaylists(userId);

        LinkedHashSet<String> importedExternalPlaylistIds = importedPlaylists.stream()
            .filter(playlist -> playlist.sourcePlatform().equals(preferredPlatform.platformId()))
            .map(PmsPlaylistImportStore.ImportedPlaylistState::externalPlaylistId)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        boolean preferredConnected = preferredConnection != null && preferredConnection.connected() && preferredCredentialReady;

        return new PmsPlaylistImportBootstrapResponse(
            "api",
            "ok",
            Instant.now(),
            new PmsPlaylistImportBootstrapResponse.ImportUser(
                account.userId(),
                account.displayName(),
                account.preferredPlatformId()
            ),
            new PmsPlaylistImportBootstrapResponse.PreferredPlatformConnection(
                preferredPlatform.platformId(),
                preferredPlatform.displayName(),
                preferredConnected,
                preferredConnection == null ? null : preferredConnection.connectionMode(),
                preferredConnection == null ? null : preferredConnection.externalAccountLabel(),
                preferredConnection != null && preferredConnection.syncReady()
            ),
            new PmsPlaylistImportBootstrapResponse.ImportSummary(
                preferredConnected,
                availablePlaylists.size(),
                importedPlaylists.size(),
                nextStepPath(preferredConnected, importedPlaylists.isEmpty()),
                nextStepMessage(preferredConnected, importedPlaylists.isEmpty())
            ),
            availablePlaylists.stream()
                .map(playlist -> new PmsPlaylistImportBootstrapResponse.AvailablePlaylist(
                    playlist.externalPlaylistId(),
                    playlist.title(),
                    playlist.sourcePlatform(),
                    playlist.trackCount(),
                    playlist.curator(),
                    playlist.description(),
                    importedExternalPlaylistIds.contains(playlist.externalPlaylistId()),
                    "complete_spotify_snapshot"
                ))
                .toList(),
            importedPlaylists.stream()
                .map(playlist -> new PmsPlaylistImportBootstrapResponse.ImportedPlaylist(
                    playlist.playlistId(),
                    playlist.externalPlaylistId(),
                    playlist.title(),
                    playlist.sourcePlatform(),
                    playlist.trackCount(),
                    playlist.importedAt()
                ))
                .toList()
        );
    }

    public PmsPlaylistImportResponse importPlaylists(PmsPlaylistImportRequest request) {
        AuthRegisteredAccount account = findAccount(request.userId());
        PlatformOption platform = findPlatform(request.platformId());
        PlatformConnectionState connectionState = findConnection(request.userId(), request.platformId())
            .filter(PlatformConnectionState::connected)
            .orElseThrow(() -> new IllegalArgumentException("Connect the selected platform before importing playlists."));
        Instant now = Instant.now();
        PlatformAccountCredential credential = findCredential(request.userId(), request.platformId())
            .filter(savedCredential -> !savedCredential.isExpired(now))
            .orElseThrow(() -> new IllegalArgumentException("Platform credential is missing. Reconnect the platform and try again."));
        PlatformPlaylistProvider provider = getProvider(request.platformId(), credential);

        List<String> requestedPlaylistIds = request.externalPlaylistIds().stream()
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .distinct()
            .toList();

        if (requestedPlaylistIds.isEmpty()) {
            throw new IllegalArgumentException("Select at least one platform playlist to import into PMS.");
        }

        Map<String, ImportCandidatePlaylist> availablePlaylistsById = provider.listImportablePlaylists(account, credential)
            .stream()
            .collect(Collectors.toMap(
                ImportCandidatePlaylist::externalPlaylistId,
                Function.identity(),
                (left, right) -> left,
                LinkedHashMap::new
            ));

        requestedPlaylistIds.forEach(playlistId -> {
            if (!availablePlaylistsById.containsKey(playlistId)) {
                throw new IllegalArgumentException("Platform playlist is not available for import: %s".formatted(playlistId));
            }
        });

        Map<String, ImportCandidatePlaylist> importedCatalogById = provider.loadPlaylistsForImport(
            account,
            credential,
            requestedPlaylistIds
        ).stream()
            .collect(Collectors.toMap(
                ImportCandidatePlaylist::externalPlaylistId,
                Function.identity(),
                (left, right) -> left,
                LinkedHashMap::new
            ));

        Instant importedAt = now;
        List<PmsPlaylistImportStore.ImportedPlaylistState> importedPlaylists = requestedPlaylistIds.stream()
            .map(playlistId -> Optional.ofNullable(importedCatalogById.get(playlistId))
                .orElseThrow(() -> new IllegalArgumentException("Platform playlist could not be loaded for import: %s".formatted(playlistId))))
            .peek(playlist -> {
                if (playlist.tracks().isEmpty()) {
                    throw new IllegalArgumentException(
                        "Platform playlist does not contain importable tracks: %s".formatted(playlist.externalPlaylistId())
                    );
                }
            })
            .map(playlist -> toImportedPlaylistState(account.userId(), playlist, importedAt))
            .toList();

        pmsPlaylistImportStore.saveImportedPlaylists(account.userId(), importedPlaylists);

        int importedTrackCount = importedPlaylists.stream()
            .mapToInt(PmsPlaylistImportStore.ImportedPlaylistState::trackCount)
            .sum();
        int completeSpotifyAudioFeatureTrackCount = (int) importedPlaylists.stream()
            .flatMap(playlist -> playlist.tracks().stream())
            .filter(track -> track.spotifyAudioFeatures() != null && track.spotifyAudioFeatures().isComplete())
            .count();

        return new PmsPlaylistImportResponse(
            "api",
            "playlists_imported",
            importedAt,
            new PmsPlaylistImportResponse.ImportResult(
                account.userId(),
                platform.platformId(),
                platform.displayName(),
                importedPlaylists.size(),
                importedTrackCount,
                completeSpotifyAudioFeatureTrackCount,
                connectionState.connectionMode()
            ),
            importedPlaylists.stream()
                .map(playlist -> new PmsPlaylistImportResponse.ImportedPlaylistResult(
                    playlist.playlistId(),
                    playlist.externalPlaylistId(),
                    playlist.title(),
                    playlist.sourcePlatform(),
                    playlist.trackCount(),
                    playlist.importedAt()
                ))
                .toList(),
            new PmsPlaylistImportResponse.NextStep(
                "/ems",
                "Playlists were imported into PMS with complete Spotify audio feature snapshots. Continue to EMS analysis."
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

    private Optional<PlatformConnectionState> findConnection(String userId, String platformId) {
        return platformConnectionStore.findByUserId(userId).stream()
            .filter(connection -> connection.platformId().equals(platformId))
            .findFirst();
    }

    private Optional<PlatformAccountCredential> findCredential(String userId, String platformId) {
        return platformCredentialStore.findByUserIdAndPlatformId(userId, platformId);
    }

    private PlatformPlaylistProvider getProvider(String platformId, PlatformAccountCredential credential) {
        return platformPlaylistProviderRegistry.getRequiredProvider(platformId, credential);
    }

    private String nextStepPath(boolean preferredConnected, boolean hasImportedPlaylists) {
        if (!preferredConnected) {
            return "/platforms";
        }
        if (!hasImportedPlaylists) {
            return "/pms";
        }
        return "/ems";
    }

    private String nextStepMessage(boolean preferredConnected, boolean hasImportedPlaylists) {
        if (!preferredConnected) {
            return "Connect the preferred platform first so PMS can import the user's playlists.";
        }
        if (!hasImportedPlaylists) {
            return "Choose connected platform playlists and import them into PMS.";
        }
        return "PMS now has imported playlists and is ready for EMS analysis.";
    }

    private PmsPlaylistImportStore.ImportedPlaylistState toImportedPlaylistState(
        String userId,
        ImportCandidatePlaylist playlist,
        Instant importedAt
    ) {
        String playlistId = "pms-%s-%s".formatted(playlist.sourcePlatform(), playlist.externalPlaylistId());

        List<PmsPlaylistImportStore.ImportedTrackState> tracks = playlist.tracks().stream()
            .map(track -> new PmsPlaylistImportStore.ImportedTrackState(
                "pms-track-%s-%s".formatted(playlist.sourcePlatform(), track.externalTrackId()),
                track.externalTrackId(),
                track.title(),
                track.artistName(),
                playlist.sourcePlatform(),
                track.primaryGenre(),
                playlist.tracks().indexOf(track) + 1,
                track.seed(),
                track.spotifyAudioFeatures()
            ))
            .toList();

        return new PmsPlaylistImportStore.ImportedPlaylistState(
            userId,
            playlistId,
            playlist.externalPlaylistId(),
            playlist.title(),
            playlist.sourcePlatform(),
            playlist.curator(),
            playlist.description(),
            importedAt,
            tracks
        );
    }
}
