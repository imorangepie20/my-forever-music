package io.myforevermusic.api.modules.pms.application;

import io.myforevermusic.api.modules.pms.infrastructure.persistence.PmsTrackSpotifyAudioFeatures;
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
        int sortOrder,
        boolean seed,
        PmsTrackSpotifyAudioFeatures spotifyAudioFeatures
    ) {
    }
}
