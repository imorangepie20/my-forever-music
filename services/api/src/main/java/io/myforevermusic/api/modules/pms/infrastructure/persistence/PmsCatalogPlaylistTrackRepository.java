package io.myforevermusic.api.modules.pms.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PmsCatalogPlaylistTrackRepository extends JpaRepository<PmsCatalogPlaylistTrackEntity, Long> {

    List<PmsCatalogPlaylistTrackEntity> findByPlaylist_IdOrderBySortOrderAscIdAsc(String playlistId);
}
