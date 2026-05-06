package io.myforevermusic.api.modules.ems.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmsCollectedPlaylistTrackRepository extends JpaRepository<EmsCollectedPlaylistTrackEntity, Long> {
    List<EmsCollectedPlaylistTrackEntity> findByPlaylistIdOrderBySortOrderAsc(Long playlistId);
}
