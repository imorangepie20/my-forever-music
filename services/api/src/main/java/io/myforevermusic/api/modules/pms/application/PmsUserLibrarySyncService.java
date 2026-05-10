package io.myforevermusic.api.modules.pms.application;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PmsUserLibrarySyncService {

    private final PmsUserLibraryStore pmsUserLibraryStore;

    public PmsUserLibrarySyncService(PmsUserLibraryStore pmsUserLibraryStore) {
        this.pmsUserLibraryStore = pmsUserLibraryStore;
    }

    public List<PmsUserLibraryStore.LibraryPlaylistState> syncImportedPlaylists(
        String userId,
        List<PmsPlaylistImportStore.ImportedPlaylistState> importedPlaylists,
        Instant syncedAt
    ) {
        List<PmsUserLibraryStore.LibraryPlaylistState> libraryPlaylists = importedPlaylists.stream()
            .map(playlist -> new PmsUserLibraryStore.LibraryPlaylistState(
                userId,
                playlist.playlistId(),
                playlist.externalPlaylistId(),
                playlist.title(),
                playlist.sourcePlatform(),
                playlist.curator(),
                playlist.highlight(),
                playlist.coverImageUrl(),
                playlist.platformExternalUrl(),
                playlist.platformUri(),
                syncedAt,
                playlist.tracks().stream()
                    .map(track -> new PmsUserLibraryStore.LibraryTrackState(
                        track.trackId(),
                        track.externalTrackId(),
                        track.title(),
                        track.artistName(),
                        track.sourcePlatform(),
                        track.primaryGenre(),
                        track.albumTitle(),
                        track.albumImageUrl(),
                        track.platformExternalUrl(),
                        track.platformUri(),
                        track.previewUrl(),
                        track.isrc(),
                        track.spotifyTrackId(),
                        track.spotifyUri(),
                        track.tidalTrackId(),
                        track.tidalUri(),
                        track.preferredPlaybackPlatform(),
                        track.playbackTargetStatus(),
                        track.sortOrder(),
                        track.seed(),
                        track.audioFeatures()
                    ))
                    .toList()
            ))
            .toList();

        return pmsUserLibraryStore.savePlaylists(userId, libraryPlaylists);
    }
}
