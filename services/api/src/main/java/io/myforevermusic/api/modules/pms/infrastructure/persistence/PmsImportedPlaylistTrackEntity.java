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
@Table(name = "pms_imported_playlist_track")
public class PmsImportedPlaylistTrackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "imported_playlist_track_id", nullable = false)
    private Long importedPlaylistTrackId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "imported_playlist_id", nullable = false)
    private PmsImportedPlaylistEntity playlist;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "track_id", nullable = false)
    private PmsImportedTrackEntity track;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "is_seed", nullable = false)
    private boolean seed;

    protected PmsImportedPlaylistTrackEntity() {
    }

    public PmsImportedPlaylistTrackEntity(
        Long importedPlaylistTrackId,
        PmsImportedPlaylistEntity playlist,
        PmsImportedTrackEntity track,
        Integer sortOrder,
        boolean seed
    ) {
        this.importedPlaylistTrackId = importedPlaylistTrackId;
        this.playlist = playlist;
        this.track = track;
        this.sortOrder = sortOrder;
        this.seed = seed;
    }

    public Long getImportedPlaylistTrackId() {
        return importedPlaylistTrackId;
    }

    public PmsImportedPlaylistEntity getPlaylist() {
        return playlist;
    }

    public PmsImportedTrackEntity getTrack() {
        return track;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public boolean isSeed() {
        return seed;
    }
}
