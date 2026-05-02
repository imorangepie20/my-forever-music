package io.myforevermusic.api.modules.pms.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pms_playlist")
public class PmsCatalogPlaylistEntity {

    @Id
    @Column(name = "playlist_id", nullable = false, length = 100)
    private String id;

    @Column(name = "owner_user_id", nullable = false, length = 100)
    private String ownerUserId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "source_platform", nullable = false, length = 50)
    private String sourcePlatform;

    @Column(name = "track_count", nullable = false)
    private Integer trackCount;

    @Column(name = "curator", nullable = false, length = 50)
    private String curator;

    @Column(name = "highlight", nullable = false, length = 500)
    private String highlight;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    protected PmsCatalogPlaylistEntity() {
    }

    public PmsCatalogPlaylistEntity(
        String id,
        String ownerUserId,
        String title,
        String sourcePlatform,
        Integer trackCount,
        String curator,
        String highlight,
        Integer displayOrder
    ) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.title = title;
        this.sourcePlatform = sourcePlatform;
        this.trackCount = trackCount;
        this.curator = curator;
        this.highlight = highlight;
        this.displayOrder = displayOrder;
    }

    public String getId() {
        return id;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public String getTitle() {
        return title;
    }

    public String getSourcePlatform() {
        return sourcePlatform;
    }

    public Integer getTrackCount() {
        return trackCount;
    }

    public String getCurator() {
        return curator;
    }

    public String getHighlight() {
        return highlight;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }
}
