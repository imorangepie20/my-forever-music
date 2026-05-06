package io.myforevermusic.api.modules.platform.infrastructure.youtubemusic;

import io.myforevermusic.api.modules.auth.application.AuthRegisteredAccount;
import io.myforevermusic.api.modules.platform.application.PlatformAccountCredential;
import io.myforevermusic.api.modules.platform.application.PlatformPlaylistProvider;
import io.myforevermusic.api.modules.pms.application.PmsPlaylistImportCatalogService.ImportCandidatePlaylist;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Placeholder for YouTube Music PMS Playlist Provider
 *
 * <p>YouTube Music does not provide an official public API for playlist access.
 * This placeholder documents the implementation approach:
 *
 * <h3>Current Status</h3>
 * <ul>
 *   <li>Official YouTube Data API v3 does NOT support YouTube Music playlists</li>
 *   <li>Third-party solutions (ytmusicapi, etc.) reverse-engineer internal APIs</li>
 *   <li>These approaches may violate YouTube Terms of Service</li>
 * </ul>
 *
 * <h3>Implementation Options</h3>
 * <ol>
 *   <li><b>Wait for official API</b> - YouTube may release official YouTube Music API</li>
 *   <li><b>Use YouTube Data API v3</b> - Limited to regular YouTube, not Music-specific</li>
 *   <li><b>Third-party library integration</b> - Requires legal/TOS review</li>
 *   <li><b>Browser extension approach</b> - User-side data export</li>
 * </ol>
 *
 * <h3>Alternative: Signal Source</h3>
 * YouTube Music could still serve as an EMS signal source through:
 * <ul>
 *   <li>Public YouTube Music playlists (if accessible)</li>
 *   <li>User's YouTube likes/watch history (via YouTube Data API)</li>
 *   <li>Trending music on YouTube (as proxy for Music trends)</li>
 * </ul>
 *
 * @see <a href="https://developers.google.com/youtube/v3">YouTube Data API v3</a>
 */
@Component
public class YouTubeMusicPlatformPlaylistProvider implements PlatformPlaylistProvider {

    private static final Logger log = LoggerFactory.getLogger(YouTubeMusicPlatformPlaylistProvider.class);
    private static final String NOT_IMPLEMENTED_MESSAGE =
        "YouTube Music PMS import is not yet implemented. " +
        "YouTube does not provide an official public API for YouTube Music playlists. " +
        "Consider using YouTube Data API v3 for general YouTube content as an EMS signal source.";

    @Override
    public boolean supports(String platformId, PlatformAccountCredential credential) {
        return "youtube-music".equals(platformId);
    }

    @Override
    public List<ImportCandidatePlaylist> listImportablePlaylists(
        AuthRegisteredAccount account,
        PlatformAccountCredential credential
    ) {
        log.warn(NOT_IMPLEMENTED_MESSAGE);
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE);
    }

    @Override
    public List<ImportCandidatePlaylist> loadPlaylistsForImport(
        AuthRegisteredAccount account,
        PlatformAccountCredential credential,
        List<String> externalPlaylistIds
    ) {
        log.warn(NOT_IMPLEMENTED_MESSAGE);
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE);
    }
}
