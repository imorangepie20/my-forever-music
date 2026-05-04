package io.myforevermusic.api.modules.pms.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PmsUserPlaylistTrackRepository extends JpaRepository<PmsUserPlaylistTrackEntity, Long> {

    List<PmsUserPlaylistTrackEntity> findByPlaylist_UserPlaylistIdOrderBySortOrderAscUserPlaylistTrackIdAsc(Long userPlaylistId);

    void deleteByPlaylist_UserPlaylistId(Long userPlaylistId);
}
