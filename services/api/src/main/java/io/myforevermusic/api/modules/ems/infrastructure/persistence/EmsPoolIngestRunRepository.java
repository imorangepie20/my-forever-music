package io.myforevermusic.api.modules.ems.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmsPoolIngestRunRepository extends JpaRepository<EmsPoolIngestRunEntity, Long> {
    Optional<EmsPoolIngestRunEntity> findFirstByStatusOrderByCreatedAtAsc(String status);
    List<EmsPoolIngestRunEntity> findTop20ByOrderByCreatedAtDesc();
}
