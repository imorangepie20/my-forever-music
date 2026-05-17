package io.myforevermusic.api.modules.ems.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmsCollectedPlaylistTrackRepository extends JpaRepository<EmsCollectedPlaylistTrackEntity, Long> {
    @Query("""
        select link
        from EmsCollectedPlaylistTrackEntity link
        join fetch link.track
        where link.playlist.id = :playlistId
        order by link.sortOrder asc
        """)
    List<EmsCollectedPlaylistTrackEntity> findByPlaylistIdOrderBySortOrderAsc(@Param("playlistId") Long playlistId);

    @Query("""
        select count(link)
        from EmsCollectedPlaylistTrackEntity link
        where link.playlist.id = :playlistId
        """)
    long countTracksByPlaylistId(@Param("playlistId") Long playlistId);

    @Query("""
        select count(link)
        from EmsCollectedPlaylistTrackEntity link
        join link.track track
        where link.playlist.id = :playlistId
          and track.audioFeatures.audioFeaturesFilled = true
        """)
    long countAudioFeatureFilledTracksByPlaylistId(@Param("playlistId") Long playlistId);

    @Query("""
        select link.playlist.id as playlistId,
               count(link.id) as trackCount,
               sum(case when link.track.audioFeatures.audioFeaturesFilled = true then 1 else 0 end) as filledTrackCount,
               avg(case when link.track.audioFeatures.audioFeaturesFilled = true then link.track.audioFeatures.energy else null end) as averageEnergy,
               avg(case when link.track.audioFeatures.audioFeaturesFilled = true then link.track.audioFeatures.valence else null end) as averageValence,
               avg(case when link.track.audioFeatures.audioFeaturesFilled = true then link.track.audioFeatures.danceability else null end) as averageDanceability,
               avg(case when link.track.audioFeatures.audioFeaturesFilled = true then link.track.audioFeatures.acousticness else null end) as averageAcousticness,
               avg(case when link.track.audioFeatures.audioFeaturesFilled = true then link.track.audioFeatures.speechiness else null end) as averageSpeechiness
        from EmsCollectedPlaylistTrackEntity link
        where link.playlist.id in :playlistIds
        group by link.playlist.id
        """)
    List<PlaylistAudioStatsProjection> findAudioStatsByPlaylistIds(@Param("playlistIds") List<Long> playlistIds);

    @Modifying
    @Query(value = """
        insert into ems_collected_playlist_track (
            ems_collected_playlist_id,
            ems_collected_track_id,
            sort_order
        ) values (
            :playlistId,
            :trackId,
            :sortOrder
        )
        on conflict (ems_collected_playlist_id, ems_collected_track_id) do update set
            sort_order = excluded.sort_order
        """, nativeQuery = true)
    void upsertPlaylistTrackLink(
        @Param("playlistId") Long playlistId,
        @Param("trackId") Long trackId,
        @Param("sortOrder") int sortOrder
    );

    @Modifying
    @Query("delete from EmsCollectedPlaylistTrackEntity link where link.playlist.id = :playlistId")
    int deleteByPlaylistId(@Param("playlistId") Long playlistId);

    interface PlaylistAudioStatsProjection {
        Long getPlaylistId();
        long getTrackCount();
        Long getFilledTrackCount();
        Double getAverageEnergy();
        Double getAverageValence();
        Double getAverageDanceability();
        Double getAverageAcousticness();
        Double getAverageSpeechiness();
    }
}
