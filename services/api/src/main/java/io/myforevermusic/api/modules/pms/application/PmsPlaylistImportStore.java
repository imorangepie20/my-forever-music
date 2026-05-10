package io.myforevermusic.api.modules.pms.application;

import io.myforevermusic.api.modules.pms.infrastructure.persistence.PmsTrackAudioFeatures;
import java.time.Instant;
import java.util.List;

public interface PmsPlaylistImportStore {

    List<ImportedPlaylistState> findImportedPlaylists(String userId);

    List<ImportedPlaylistState> saveImportedPlaylists(String userId, List<ImportedPlaylistState> playlists);

    record ImportedPlaylistState(
        String userId,
        String playlistId,
        String externalPlaylistId,
        String title,
        String sourcePlatform,
        String curator,
        String highlight,
        String coverImageUrl,
        String platformExternalUrl,
        String platformUri,
        Instant importedAt,
        List<ImportedTrackState> tracks
    ) {

        public int trackCount() {
            return tracks == null ? 0 : tracks.size();
        }
    }

    record ImportedTrackState(
        String trackId,
        String externalTrackId,
        String title,
        String artistName,
        String sourcePlatform,
        String primaryGenre,
        String albumTitle,
        String albumImageUrl,
        String platformExternalUrl,
        String platformUri,
        String previewUrl,
        String isrc,
        String spotifyTrackId,
        String spotifyUri,
        String tidalTrackId,
        String tidalUri,
        String preferredPlaybackPlatform,
        String playbackTargetStatus,
        int sortOrder,
        boolean seed,
        PmsTrackAudioFeatures audioFeatures
    ) {
        public ImportedTrackState {
            spotifyTrackId = firstNonBlank(spotifyTrackId, nativeTrackId(sourcePlatform, externalTrackId, "spotify"));
            spotifyUri = firstNonBlank(spotifyUri, nativeUri(sourcePlatform, platformUri, "spotify"));
            tidalTrackId = firstNonBlank(tidalTrackId, nativeTrackId(sourcePlatform, externalTrackId, "tidal"));
            tidalUri = firstNonBlank(tidalUri, nativeUri(sourcePlatform, platformUri, "tidal"));
            preferredPlaybackPlatform = firstNonBlank(
                preferredPlaybackPlatform,
                firstNonBlank(nativePlatform(sourcePlatform), firstNonBlank(hasText(tidalTrackId) ? "tidal" : null, hasText(spotifyTrackId) ? "spotify" : null))
            );
            playbackTargetStatus = firstNonBlank(
                playbackTargetStatus,
                hasText(nativePlatform(sourcePlatform)) ? "native" : (hasText(spotifyTrackId) || hasText(tidalTrackId) ? "resolved" : "unresolved")
            );
        }

        public ImportedTrackState(
            String trackId,
            String externalTrackId,
            String title,
            String artistName,
            String sourcePlatform,
            String primaryGenre,
            String albumTitle,
            String albumImageUrl,
            String platformExternalUrl,
            String platformUri,
            String previewUrl,
            int sortOrder,
            boolean seed,
            PmsTrackAudioFeatures audioFeatures
        ) {
            this(
                trackId,
                externalTrackId,
                title,
                artistName,
                sourcePlatform,
                primaryGenre,
                albumTitle,
                albumImageUrl,
                platformExternalUrl,
                platformUri,
                previewUrl,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                sortOrder,
                seed,
                audioFeatures
            );
        }
    }

    private static String nativeTrackId(String sourcePlatform, String externalTrackId, String targetPlatform) {
        return targetPlatform.equals(sourcePlatform) ? externalTrackId : null;
    }

    private static String nativeUri(String sourcePlatform, String platformUri, String targetPlatform) {
        return targetPlatform.equals(sourcePlatform) ? platformUri : null;
    }

    private static String nativePlatform(String sourcePlatform) {
        return "spotify".equals(sourcePlatform) || "tidal".equals(sourcePlatform) ? sourcePlatform : null;
    }

    private static String firstNonBlank(String first, String second) {
        return hasText(first) ? first : (hasText(second) ? second : null);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
