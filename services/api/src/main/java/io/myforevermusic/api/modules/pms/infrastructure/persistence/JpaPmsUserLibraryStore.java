package io.myforevermusic.api.modules.pms.infrastructure.persistence;

import io.myforevermusic.api.modules.pms.application.PmsUserLibraryStore;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!local")
public class JpaPmsUserLibraryStore implements PmsUserLibraryStore {

    private final PmsUserPlaylistRepository playlistRepository;
    private final PmsUserTrackRepository trackRepository;
    private final PmsUserPlaylistTrackRepository playlistTrackRepository;

    public JpaPmsUserLibraryStore(
        PmsUserPlaylistRepository playlistRepository,
        PmsUserTrackRepository trackRepository,
        PmsUserPlaylistTrackRepository playlistTrackRepository
    ) {
        this.playlistRepository = playlistRepository;
        this.trackRepository = trackRepository;
        this.playlistTrackRepository = playlistTrackRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LibraryPlaylistState> findPlaylists(String userId) {
        return playlistRepository.findAllByUserIdOrderByLastSyncedAtDescPlaylistIdAsc(userId).stream()
            .map(this::toState)
            .toList();
    }

    @Override
    @Transactional
    public List<LibraryPlaylistState> savePlaylists(String userId, List<LibraryPlaylistState> playlists) {
        for (LibraryPlaylistState playlistState : playlists) {
            PmsUserPlaylistEntity playlistEntity = playlistRepository
                .findByUserIdAndPlaylistId(userId, playlistState.playlistId())
                .orElseGet(() -> new PmsUserPlaylistEntity(userId, playlistState));
            playlistEntity.apply(userId, playlistState);
            PmsUserPlaylistEntity savedPlaylist = playlistRepository.save(playlistEntity);

            playlistTrackRepository.deleteByPlaylist_UserPlaylistId(savedPlaylist.getUserPlaylistId());

            List<PmsUserPlaylistTrackEntity> trackLinks = playlistState.tracks().stream()
                .map(trackState -> new PmsUserPlaylistTrackEntity(
                    null,
                    savedPlaylist,
                    saveTrack(trackState),
                    trackState.sortOrder(),
                    trackState.seed()
                ))
                .toList();

            playlistTrackRepository.saveAll(trackLinks);
        }

        return findPlaylists(userId);
    }

    private PmsUserTrackEntity saveTrack(LibraryTrackState trackState) {
        PmsUserTrackEntity trackEntity = trackRepository.findById(trackState.trackId())
            .orElseGet(() -> new PmsUserTrackEntity(trackState));
        trackEntity.apply(trackState);
        return trackRepository.save(trackEntity);
    }

    private LibraryPlaylistState toState(PmsUserPlaylistEntity playlistEntity) {
        List<LibraryTrackState> tracks = playlistTrackRepository
            .findByPlaylist_UserPlaylistIdOrderBySortOrderAscUserPlaylistTrackIdAsc(
                playlistEntity.getUserPlaylistId()
            ).stream()
            .map(this::toTrackState)
            .toList();

        return new LibraryPlaylistState(
            playlistEntity.getUserId(),
            playlistEntity.getPlaylistId(),
            playlistEntity.getExternalPlaylistId(),
            playlistEntity.getTitle(),
            playlistEntity.getSourcePlatform(),
            playlistEntity.getCurator(),
            playlistEntity.getHighlight(),
            playlistEntity.getCoverImageUrl(),
            playlistEntity.getPlatformExternalUrl(),
            playlistEntity.getPlatformUri(),
            playlistEntity.getLastSyncedAt(),
            tracks
        );
    }

    private LibraryTrackState toTrackState(PmsUserPlaylistTrackEntity playlistTrackEntity) {
        PmsUserTrackEntity trackEntity = playlistTrackEntity.getTrack();

        return new LibraryTrackState(
            trackEntity.getTrackId(),
            trackEntity.getExternalTrackId(),
            trackEntity.getTitle(),
            trackEntity.getArtistName(),
            trackEntity.getSourcePlatform(),
            trackEntity.getPrimaryGenre(),
            trackEntity.getAlbumTitle(),
            trackEntity.getAlbumImageUrl(),
            trackEntity.getPlatformExternalUrl(),
            trackEntity.getPlatformUri(),
            trackEntity.getPreviewUrl(),
            playlistTrackEntity.getSortOrder(),
            playlistTrackEntity.isSeed(),
            trackEntity.getAudioFeatures()
        );
    }
}
