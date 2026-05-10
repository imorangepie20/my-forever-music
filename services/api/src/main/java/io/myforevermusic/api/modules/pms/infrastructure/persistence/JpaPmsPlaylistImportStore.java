package io.myforevermusic.api.modules.pms.infrastructure.persistence;

import io.myforevermusic.api.modules.pms.application.PmsPlaylistImportStore;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!local")
public class JpaPmsPlaylistImportStore implements PmsPlaylistImportStore {

    private final PmsImportedPlaylistRepository playlistRepository;
    private final PmsImportedTrackRepository trackRepository;
    private final PmsImportedPlaylistTrackRepository playlistTrackRepository;

    public JpaPmsPlaylistImportStore(
        PmsImportedPlaylistRepository playlistRepository,
        PmsImportedTrackRepository trackRepository,
        PmsImportedPlaylistTrackRepository playlistTrackRepository
    ) {
        this.playlistRepository = playlistRepository;
        this.trackRepository = trackRepository;
        this.playlistTrackRepository = playlistTrackRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImportedPlaylistState> findImportedPlaylists(String userId) {
        return playlistRepository.findAllByUserIdOrderByImportedAtDescPlaylistIdAsc(userId).stream()
            .map(this::toState)
            .toList();
    }

    @Override
    @Transactional
    public List<ImportedPlaylistState> saveImportedPlaylists(String userId, List<ImportedPlaylistState> playlists) {
        for (ImportedPlaylistState playlistState : playlists) {
            PmsImportedPlaylistEntity playlistEntity = playlistRepository
                .findByUserIdAndPlaylistId(userId, playlistState.playlistId())
                .orElseGet(() -> new PmsImportedPlaylistEntity(userId, playlistState));
            playlistEntity.apply(userId, playlistState);
            PmsImportedPlaylistEntity savedPlaylist = playlistRepository.save(playlistEntity);

            playlistTrackRepository.deleteByPlaylist_ImportedPlaylistId(savedPlaylist.getImportedPlaylistId());

            List<PmsImportedPlaylistTrackEntity> trackLinks = playlistState.tracks().stream()
                .map(trackState -> new PmsImportedPlaylistTrackEntity(
                    null,
                    savedPlaylist,
                    saveTrack(trackState),
                    trackState.sortOrder(),
                    trackState.seed()
                ))
                .toList();

            playlistTrackRepository.saveAll(trackLinks);
        }

        return findImportedPlaylists(userId);
    }

    private PmsImportedTrackEntity saveTrack(ImportedTrackState trackState) {
        PmsImportedTrackEntity trackEntity = trackRepository.findById(trackState.trackId())
            .orElseGet(() -> new PmsImportedTrackEntity(trackState));
        trackEntity.apply(trackState);
        return trackRepository.save(trackEntity);
    }

    private ImportedPlaylistState toState(PmsImportedPlaylistEntity playlistEntity) {
        List<ImportedTrackState> tracks = playlistTrackRepository
            .findByPlaylist_ImportedPlaylistIdOrderBySortOrderAscImportedPlaylistTrackIdAsc(
                playlistEntity.getImportedPlaylistId()
            ).stream()
            .map(this::toTrackState)
            .toList();

        return new ImportedPlaylistState(
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
            playlistEntity.getImportedAt(),
            tracks
        );
    }

    private ImportedTrackState toTrackState(PmsImportedPlaylistTrackEntity playlistTrackEntity) {
        PmsImportedTrackEntity trackEntity = playlistTrackEntity.getTrack();

        return new ImportedTrackState(
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
            trackEntity.getIsrc(),
            trackEntity.getSpotifyTrackId(),
            trackEntity.getSpotifyUri(),
            trackEntity.getTidalTrackId(),
            trackEntity.getTidalUri(),
            trackEntity.getPreferredPlaybackPlatform(),
            trackEntity.getPlaybackTargetStatus(),
            playlistTrackEntity.getSortOrder(),
            playlistTrackEntity.isSeed(),
            trackEntity.getAudioFeatures()
        );
    }
}
