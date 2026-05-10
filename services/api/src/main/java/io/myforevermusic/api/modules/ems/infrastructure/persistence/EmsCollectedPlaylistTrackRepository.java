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
}
