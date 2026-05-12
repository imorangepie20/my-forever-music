package io.myforevermusic.api.modules.ems.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmsPoolEntryRepository extends JpaRepository<EmsPoolEntryEntity, Long> {
    @Query("select entry from EmsPoolEntryEntity entry where entry.run.id = :runId and entry.status = :status order by entry.id")
    List<EmsPoolEntryEntity> findByRunIdAndStatus(
        @Param("runId") Long runId,
        @Param("status") String status,
        Pageable pageable
    );

    @Query("select entry from EmsPoolEntryEntity entry where entry.run.id = :runId order by entry.id")
    List<EmsPoolEntryEntity> findByRunId(@Param("runId") Long runId, Pageable pageable);

    @Query("select count(entry) from EmsPoolEntryEntity entry where entry.run.id = :runId and entry.entryType = :entryType and entry.status = :status")
    long countByRunIdAndEntryTypeAndStatus(
        @Param("runId") Long runId,
        @Param("entryType") String entryType,
        @Param("status") String status
    );

    @Query("select count(entry) from EmsPoolEntryEntity entry where entry.run.id = :runId and entry.status = :status")
    long countByRunIdAndStatus(@Param("runId") Long runId, @Param("status") String status);

    @Query("select count(entry) from EmsPoolEntryEntity entry where entry.run.id = :runId")
    long countByRunId(@Param("runId") Long runId);

    @Query(
        value = """
            select * from ems_pool_entry
            where ems_pool_ingest_run_id = :runId and status = :status
            order by ems_pool_entry_id
            limit :limit
            for update skip locked
            """,
        nativeQuery = true
    )
    List<EmsPoolEntryEntity> findClaimableEntriesForUpdate(
        @Param("runId") Long runId,
        @Param("status") String status,
        @Param("limit") int limit
    );

    @Modifying
    @Query("update EmsPoolEntryEntity entry set entry.status = :toStatus, entry.updatedAt = :now "
        + "where entry.run.id = :runId and entry.status = :fromStatus")
    int updateStatusByRunIdAndStatus(
        @Param("runId") Long runId,
        @Param("fromStatus") String fromStatus,
        @Param("toStatus") String toStatus,
        @Param("now") Instant now
    );
}
