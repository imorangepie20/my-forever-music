package io.myforevermusic.api.modules.pms.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PmsPersonalPlaylistTrackRepository extends JpaRepository<PmsPersonalPlaylistTrackEntity, Long> {

    List<PmsPersonalPlaylistTrackEntity> findByPlaylist_PersonalPlaylistIdOrderBySortOrderAscPersonalPlaylistTrackIdAsc(
        Long personalPlaylistId
    );

    Optional<PmsPersonalPlaylistTrackEntity> findByPlaylist_PersonalPlaylistIdAndTrack_TrackId(
        Long personalPlaylistId,
        String trackId
    );

    long countByPlaylist_PersonalPlaylistId(Long personalPlaylistId);
}
