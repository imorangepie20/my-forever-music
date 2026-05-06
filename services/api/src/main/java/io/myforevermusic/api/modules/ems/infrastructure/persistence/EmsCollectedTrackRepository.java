package io.myforevermusic.api.modules.ems.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmsCollectedTrackRepository extends JpaRepository<EmsCollectedTrackEntity, Long> {
    List<EmsCollectedTrackEntity> findBySourcePlatformOrderByCollectedAtDesc(String platformId);
    Optional<EmsCollectedTrackEntity> findBySourcePlatformAndExternalTrackId(String platformId, String externalTrackId);
}
