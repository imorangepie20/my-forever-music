package io.myforevermusic.api.modules.pms.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PmsImportedPlaylistTrackRepository extends JpaRepository<PmsImportedPlaylistTrackEntity, Long> {

    List<PmsImportedPlaylistTrackEntity> findByPlaylist_ImportedPlaylistIdOrderBySortOrderAscImportedPlaylistTrackIdAsc(
        Long importedPlaylistId
    );

    void deleteByPlaylist_ImportedPlaylistId(Long importedPlaylistId);
}
