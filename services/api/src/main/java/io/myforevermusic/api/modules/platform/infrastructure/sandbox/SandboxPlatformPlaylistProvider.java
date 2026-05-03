package io.myforevermusic.api.modules.platform.infrastructure.sandbox;

import io.myforevermusic.api.modules.auth.application.AuthRegisteredAccount;
import io.myforevermusic.api.modules.platform.application.PlatformAccountCredential;
import io.myforevermusic.api.modules.platform.application.PlatformPlaylistProvider;
import io.myforevermusic.api.modules.pms.application.PmsPlaylistImportCatalogService;
import io.myforevermusic.api.modules.pms.application.PmsPlaylistImportCatalogService.ImportCandidatePlaylist;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SandboxPlatformPlaylistProvider implements PlatformPlaylistProvider {

    private final PmsPlaylistImportCatalogService pmsPlaylistImportCatalogService;

    public SandboxPlatformPlaylistProvider(PmsPlaylistImportCatalogService pmsPlaylistImportCatalogService) {
        this.pmsPlaylistImportCatalogService = pmsPlaylistImportCatalogService;
    }

    @Override
    public boolean supports(String platformId, PlatformAccountCredential credential) {
        if ("apple-music".equals(platformId) || "tidal".equals(platformId) || "youtube-music".equals(platformId)) {
            return true;
        }

        return "spotify".equals(platformId)
            && credential != null
            && credential.authorizationMode() != null
            && credential.authorizationMode().startsWith("sandbox");
    }

    @Override
    public List<ImportCandidatePlaylist> listImportablePlaylists(
        AuthRegisteredAccount account,
        PlatformAccountCredential credential
    ) {
        return pmsPlaylistImportCatalogService.getAvailablePlaylists(credential.platformId()).stream()
            .map(ImportCandidatePlaylist::withoutTracks)
            .toList();
    }

    @Override
    public List<ImportCandidatePlaylist> loadPlaylistsForImport(
        AuthRegisteredAccount account,
        PlatformAccountCredential credential,
        List<String> externalPlaylistIds
    ) {
        return pmsPlaylistImportCatalogService.getAvailablePlaylists(credential.platformId()).stream()
            .filter(playlist -> externalPlaylistIds.contains(playlist.externalPlaylistId()))
            .toList();
    }
}
