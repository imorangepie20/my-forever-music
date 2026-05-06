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
import java.time.Instant;

@Entity
@Table(name = "pms_personal_playlist_track")
public class PmsPersonalPlaylistTrackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "personal_playlist_track_id", nullable = false)
    private Long personalPlaylistTrackId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "personal_playlist_id", nullable = false)
    private PmsPersonalPlaylistEntity playlist;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "track_id", nullable = false)
    private PmsUserTrackEntity track;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "source_context", nullable = false, length = 80)
    private String sourceContext;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    protected PmsPersonalPlaylistTrackEntity() {
    }

    public PmsPersonalPlaylistTrackEntity(
        Long personalPlaylistTrackId,
        PmsPersonalPlaylistEntity playlist,
        PmsUserTrackEntity track,
        Integer sortOrder,
        String sourceContext,
        Instant addedAt
    ) {
        this.personalPlaylistTrackId = personalPlaylistTrackId;
        this.playlist = playlist;
        this.track = track;
        this.sortOrder = sortOrder;
        this.sourceContext = sourceContext;
        this.addedAt = addedAt;
    }

    public Long getPersonalPlaylistTrackId() {
        return personalPlaylistTrackId;
    }

    public PmsPersonalPlaylistEntity getPlaylist() {
        return playlist;
    }

    public PmsUserTrackEntity getTrack() {
        return track;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public String getSourceContext() {
        return sourceContext;
    }

    public Instant getAddedAt() {
        return addedAt;
    }
}
