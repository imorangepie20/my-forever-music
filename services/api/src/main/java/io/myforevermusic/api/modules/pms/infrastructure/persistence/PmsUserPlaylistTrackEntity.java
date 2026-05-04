package io.myforevermusic.api.modules.pms.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "pms_user_playlist_track")
public class PmsUserPlaylistTrackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_playlist_track_id", nullable = false)
    private Long userPlaylistTrackId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_playlist_id", nullable = false)
    private PmsUserPlaylistEntity playlist;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "track_id", nullable = false)
    private PmsUserTrackEntity track;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "is_seed", nullable = false)
    private boolean seed;

    protected PmsUserPlaylistTrackEntity() {
    }

    public PmsUserPlaylistTrackEntity(
        Long userPlaylistTrackId,
        PmsUserPlaylistEntity playlist,
        PmsUserTrackEntity track,
        Integer sortOrder,
        boolean seed
    ) {
        this.userPlaylistTrackId = userPlaylistTrackId;
        this.playlist = playlist;
        this.track = track;
        this.sortOrder = sortOrder;
        this.seed = seed;
    }

    public Long getUserPlaylistTrackId() {
        return userPlaylistTrackId;
    }

    public PmsUserPlaylistEntity getPlaylist() {
        return playlist;
    }

    public PmsUserTrackEntity getTrack() {
        return track;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public boolean isSeed() {
        return seed;
    }
}
