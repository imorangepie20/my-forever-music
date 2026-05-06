package io.myforevermusic.api.modules.pms.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PmsPersonalPlaylistStore {

    List<PersonalPlaylistState> findPlaylists(String userId);

    Optional<PersonalPlaylistState> findPlaylist(String userId, String playlistId);

    PersonalPlaylistState createPlaylist(CreatePlaylistDraft draft);

    PersonalPlaylistState addTrack(AddTrackDraft draft);

    record CreatePlaylistDraft(
        String userId,
        String playlistId,
        String title,
        String description
    ) {
    }

    record AddTrackDraft(
        String userId,
        String playlistId,
        PersonalTrackState track,
        String sourceContext
    ) {
    }

    record PersonalPlaylistState(
        String userId,
        String playlistId,
        String title,
        String description,
        Instant createdAt,
        Instant updatedAt,
        List<PersonalTrackState> tracks
    ) {

        public int trackCount() {
            return tracks == null ? 0 : tracks.size();
        }
    }

    record PersonalTrackState(
        String trackId,
        String title,
        String artistName,
        String sourcePlatform,
        String albumTitle,
        String albumImageUrl,
        String platformExternalUrl,
        String platformUri,
        String previewUrl,
        String spotifyTrackId,
        Integer durationMs,
        Integer sortOrder,
        String sourceContext,
        Instant addedAt
    ) {
        public String audioFeatureTrackId() {
            return spotifyTrackId;
        }
    }
}
