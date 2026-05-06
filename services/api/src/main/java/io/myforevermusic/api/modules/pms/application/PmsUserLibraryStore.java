package io.myforevermusic.api.modules.pms.application;

import io.myforevermusic.api.modules.pms.infrastructure.persistence.PmsTrackAudioFeatures;
import java.time.Instant;
import java.util.List;

public interface PmsUserLibraryStore {

    List<LibraryPlaylistState> findPlaylists(String userId);

    List<LibraryPlaylistState> savePlaylists(String userId, List<LibraryPlaylistState> playlists);

    record LibraryPlaylistState(
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
        Instant lastSyncedAt,
        List<LibraryTrackState> tracks
    ) {

        public int trackCount() {
            return tracks == null ? 0 : tracks.size();
        }
    }

    record LibraryTrackState(
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
    }
}
