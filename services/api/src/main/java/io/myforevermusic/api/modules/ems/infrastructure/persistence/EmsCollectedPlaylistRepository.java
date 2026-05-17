package io.myforevermusic.api.modules.ems.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Query("""
        select playlist
        from EmsCollectedPlaylistEntity playlist
        where exists (
            select link.id
            from EmsCollectedPlaylistTrackEntity link
            where link.playlist.id = playlist.id
        )
        order by playlist.collectedAt desc
        """)
    List<EmsCollectedPlaylistEntity> findRecentWithTracks(Pageable pageable);

    @Query("""
        select playlist
        from EmsCollectedPlaylistEntity playlist
        where playlist.trackCount > 0
        order by playlist.trackCount desc, playlist.collectedAt desc
        """)
    List<EmsCollectedPlaylistEntity> findPopularByTrackCount(Pageable pageable);

    @Query("""
        select playlist
        from EmsCollectedPlaylistEntity playlist
        where playlist.sourcePlatform in :platformIds
          and exists (
              select link.id
              from EmsCollectedPlaylistTrackEntity link
              where link.playlist.id = playlist.id
          )
        order by playlist.collectedAt desc
        """)
    List<EmsCollectedPlaylistEntity> findRecentWithTracksBySourcePlatforms(
        @Param("platformIds") List<String> platformIds,
        Pageable pageable
    );

    long countBySourcePlatform(String platformId);

    Optional<EmsCollectedPlaylistEntity> findFirstBySourcePlatformOrderByCollectedAtDesc(String platformId);

    @Query(
        value = """
            select p.ems_collected_playlist_id
            from ems_collected_playlist p
            where not exists (
                select 1
                from ems_collected_playlist_track pt
                where pt.ems_collected_playlist_id = p.ems_collected_playlist_id
            )
            """,
        nativeQuery = true
    )
    List<Long> findIdsWithoutTracks();

    @Modifying
    @Query(
        value = """
            delete from ems_collected_playlist p
            where not exists (
                select 1
                from ems_collected_playlist_track pt
                where pt.ems_collected_playlist_id = p.ems_collected_playlist_id
            )
            """,
        nativeQuery = true
    )
    int deletePlaylistsWithoutTracks();
}
