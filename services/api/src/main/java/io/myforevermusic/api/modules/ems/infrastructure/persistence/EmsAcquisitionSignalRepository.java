package io.myforevermusic.api.modules.ems.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmsAcquisitionSignalRepository extends JpaRepository<EmsAcquisitionSignalEntity, Long> {
    @Query("select signal from EmsAcquisitionSignalEntity signal where signal.run.id = :runId order by signal.id")
    List<EmsAcquisitionSignalEntity> findTop50ByRunIdOrderByIdAsc(@Param("runId") Long runId);

    boolean existsByArticleUrl(String articleUrl);

    @Query("""
        select signal from EmsAcquisitionSignalEntity signal
        where signal.articleUrl is not null
          and signal.articleTitle is not null
          and signal.articleUrl <> ''
          and signal.articleTitle <> ''
        order by signal.createdAt desc, signal.id desc
        """)
    List<EmsAcquisitionSignalEntity> findRecentArticles(Pageable pageable);

    @Query("""
        select signal.sourceName as sourceName,
               count(signal) as signalCount,
               avg(signal.confidenceScore) as avgConfidence,
               max(signal.createdAt) as lastSignalAt
        from EmsAcquisitionSignalEntity signal
        where signal.createdAt >= :since
        group by signal.sourceName
        order by count(signal) desc
        """)
    List<SourceQualityRow> summarizeSourceQualitySince(@Param("since") Instant since);

    interface SourceQualityRow {
        String getSourceName();
        long getSignalCount();
        Double getAvgConfidence();
        Instant getLastSignalAt();
    }
}
