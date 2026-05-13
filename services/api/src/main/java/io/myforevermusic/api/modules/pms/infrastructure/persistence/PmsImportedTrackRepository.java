package io.myforevermusic.api.modules.pms.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PmsImportedTrackRepository extends JpaRepository<PmsImportedTrackEntity, String> {

    @Modifying
    @Query("""
        update PmsImportedTrackEntity track
        set track.canonicalTrackId = :canonicalTrackId
        where lower(track.isrc) = lower(:isrc)
          and track.canonicalTrackId is null
        """)
    int linkCanonicalTrackByIsrc(@Param("isrc") String isrc, @Param("canonicalTrackId") Long canonicalTrackId);

    @Query("""
        select count(track)
        from PmsImportedTrackEntity track
        where lower(track.isrc) = lower(:isrc)
          and track.canonicalTrackId is not null
          and track.canonicalTrackId <> :canonicalTrackId
        """)
    long countCanonicalTrackConflictsByIsrc(@Param("isrc") String isrc, @Param("canonicalTrackId") Long canonicalTrackId);

    @Query("""
        select track
        from PmsImportedTrackEntity track
        where lower(track.isrc) = lower(:isrc)
          and track.canonicalTrackId is not null
          and track.canonicalTrackId <> :canonicalTrackId
        order by track.canonicalTrackId asc, track.trackId asc
        """)
    java.util.List<PmsImportedTrackEntity> findCanonicalTrackConflictsByIsrc(
        @Param("isrc") String isrc,
        @Param("canonicalTrackId") Long canonicalTrackId
    );
}
