package io.myforevermusic.api.modules.ems.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmsAcquisitionSignalRepository extends JpaRepository<EmsAcquisitionSignalEntity, Long> {
    @Query("select signal from EmsAcquisitionSignalEntity signal where signal.run.id = :runId order by signal.id")
    List<EmsAcquisitionSignalEntity> findTop50ByRunIdOrderByIdAsc(@Param("runId") Long runId);
}
