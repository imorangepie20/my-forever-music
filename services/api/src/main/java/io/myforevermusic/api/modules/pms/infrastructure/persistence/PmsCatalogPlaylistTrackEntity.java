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
@Table(name = "pms_playlist_track")
public class PmsCatalogPlaylistTrackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "playlist_track_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "playlist_id", nullable = false)
    private PmsCatalogPlaylistEntity playlist;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "track_id", nullable = false)
    private PmsCatalogTrackEntity track;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "is_seed", nullable = false)
    private boolean seed;

    protected PmsCatalogPlaylistTrackEntity() {
    }

    public PmsCatalogPlaylistTrackEntity(
        Long id,
        PmsCatalogPlaylistEntity playlist,
        PmsCatalogTrackEntity track,
        Integer sortOrder,
        boolean seed
    ) {
        this.id = id;
        this.playlist = playlist;
        this.track = track;
        this.sortOrder = sortOrder;
        this.seed = seed;
    }

    public Long getId() {
        return id;
    }

    public PmsCatalogPlaylistEntity getPlaylist() {
        return playlist;
    }

    public PmsCatalogTrackEntity getTrack() {
        return track;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public boolean isSeed() {
        return seed;
    }
}
