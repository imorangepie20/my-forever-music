package io.myforevermusic.api.modules.ems.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "ems_collected_playlist_track", uniqueConstraints = {
    @UniqueConstraint(name = "uk_ems_collected_playlist_track", columnNames = {"ems_collected_playlist_id", "ems_collected_track_id"})
})
public class EmsCollectedPlaylistTrackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ems_collected_playlist_track_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ems_collected_playlist_id", nullable = false)
    private EmsCollectedPlaylistEntity playlist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ems_collected_track_id", nullable = false)
    private EmsCollectedTrackEntity track;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected EmsCollectedPlaylistTrackEntity() {}

    public EmsCollectedPlaylistTrackEntity(
        EmsCollectedPlaylistEntity playlist, EmsCollectedTrackEntity track, int sortOrder
    ) {
        this.playlist = playlist;
        this.track = track;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public EmsCollectedPlaylistEntity getPlaylist() { return playlist; }
    public EmsCollectedTrackEntity getTrack() { return track; }
    public int getSortOrder() { return sortOrder; }
}
