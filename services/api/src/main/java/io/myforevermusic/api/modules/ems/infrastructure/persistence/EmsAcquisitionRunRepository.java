package io.myforevermusic.api.modules.ems.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmsAcquisitionRunRepository extends JpaRepository<EmsAcquisitionRunEntity, Long> {
    Optional<EmsAcquisitionRunEntity> findFirstByOrderByStartedAtDesc();
    List<EmsAcquisitionRunEntity> findTop20ByOrderByStartedAtDesc();
}
