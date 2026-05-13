package io.myforevermusic.api.modules.ems.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmsAcquisitionSeedRepository extends JpaRepository<EmsAcquisitionSeedEntity, Long> {
    @Query("select seed from EmsAcquisitionSeedEntity seed where seed.run.id = :runId order by seed.id")
    List<EmsAcquisitionSeedEntity> findTop100ByRunIdOrderByIdAsc(@Param("runId") Long runId);
}
