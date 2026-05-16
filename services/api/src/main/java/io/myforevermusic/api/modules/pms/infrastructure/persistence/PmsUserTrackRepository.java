package io.myforevermusic.api.modules.pms.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PmsUserTrackRepository extends JpaRepository<PmsUserTrackEntity, String> {

    @Query("""
        select track
        from PmsUserTrackEntity track
        where track.audioFeatures.audioFeatureTrackId = :audioFeatureTrackId
        order by track.audioFeatures.audioFeaturesFilled desc, track.trackId asc
        """)
    Optional<PmsUserTrackEntity> findFirstByAudioFeatureTrackId(
        @Param("audioFeatureTrackId") String audioFeatureTrackId
    );


    @Modifying
    @Query("""
        update PmsUserTrackEntity track
        set track.canonicalTrackId = :canonicalTrackId
        where lower(track.isrc) = lower(:isrc)
          and track.canonicalTrackId is null
        """)
    int linkCanonicalTrackByIsrc(@Param("isrc") String isrc, @Param("canonicalTrackId") Long canonicalTrackId);

    @Query("""
        select count(track)
        from PmsUserTrackEntity track
        where lower(track.isrc) = lower(:isrc)
          and track.canonicalTrackId is not null
          and track.canonicalTrackId <> :canonicalTrackId
        """)
    long countCanonicalTrackConflictsByIsrc(@Param("isrc") String isrc, @Param("canonicalTrackId") Long canonicalTrackId);

    @Query("""
        select track
        from PmsUserTrackEntity track
        where lower(track.isrc) = lower(:isrc)
          and track.canonicalTrackId is not null
          and track.canonicalTrackId <> :canonicalTrackId
        order by track.canonicalTrackId asc, track.trackId asc
        """)
    java.util.List<PmsUserTrackEntity> findCanonicalTrackConflictsByIsrc(
        @Param("isrc") String isrc,
        @Param("canonicalTrackId") Long canonicalTrackId
    );
}
