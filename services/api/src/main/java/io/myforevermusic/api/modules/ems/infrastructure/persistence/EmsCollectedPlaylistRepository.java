package io.myforevermusic.api.modules.ems.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmsCollectedPlaylistRepository extends JpaRepository<EmsCollectedPlaylistEntity, Long> {
    List<EmsCollectedPlaylistEntity> findBySourcePlatformOrderByCollectedAtDesc(String platformId);
    Optional<EmsCollectedPlaylistEntity> findBySourcePlatformAndExternalPlaylistId(String platformId, String externalPlaylistId);
}
