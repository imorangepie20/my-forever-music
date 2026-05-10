package io.myforevermusic.api.modules.ems.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmsCollectedPlaylistRepository extends JpaRepository<EmsCollectedPlaylistEntity, Long> {
    @Query(
        value = """
            select *
            from ems_collected_playlist
            where source_platform = :platformId
            order by collected_at desc
            limit :limit
            """,
        nativeQuery = true
    )
    List<EmsCollectedPlaylistEntity> findBySourcePlatformOrderByCollectedAtDesc(
        @Param("platformId") String platformId,
        @Param("limit") int limit
    );

    @Query(
        value = """
            select *
            from ems_collected_playlist
            where source_platform = :platformId
            order by random()
            limit :limit
            """,
        nativeQuery = true
    )
    List<EmsCollectedPlaylistEntity> findRandomBySourcePlatform(
        @Param("platformId") String platformId,
        @Param("limit") int limit
    );

    Optional<EmsCollectedPlaylistEntity> findBySourcePlatformAndExternalPlaylistId(String platformId, String externalPlaylistId);

    @Query("select distinct playlist.sourcePlatform from EmsCollectedPlaylistEntity playlist order by playlist.sourcePlatform")
    List<String> findDistinctSourcePlatforms();

    long countBySourcePlatform(String platformId);

    Optional<EmsCollectedPlaylistEntity> findFirstBySourcePlatformOrderByCollectedAtDesc(String platformId);
}
