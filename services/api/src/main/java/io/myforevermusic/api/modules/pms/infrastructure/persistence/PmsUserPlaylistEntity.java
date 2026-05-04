package io.myforevermusic.api.modules.pms.infrastructure.persistence;

import io.myforevermusic.api.modules.pms.application.PmsUserLibraryStore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "pms_user_playlist")
public class PmsUserPlaylistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_playlist_id", nullable = false)
    private Long userPlaylistId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "playlist_id", nullable = false, length = 160)
    private String playlistId;

    @Column(name = "external_playlist_id", nullable = false, length = 160)
    private String externalPlaylistId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "source_platform", nullable = false, length = 50)
    private String sourcePlatform;

    @Column(name = "curator", nullable = false, length = 120)
    private String curator;

    @Column(name = "highlight", nullable = false, length = 1000)
    private String highlight;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "platform_external_url", length = 500)
    private String platformExternalUrl;

    @Column(name = "platform_uri", length = 200)
    private String platformUri;

    @Column(name = "last_synced_at", nullable = false)
    private Instant lastSyncedAt;

    protected PmsUserPlaylistEntity() {
    }

    public PmsUserPlaylistEntity(String userId, PmsUserLibraryStore.LibraryPlaylistState state) {
        apply(userId, state);
    }

    public void apply(String userId, PmsUserLibraryStore.LibraryPlaylistState state) {
        this.userId = userId;
        this.playlistId = state.playlistId();
        this.externalPlaylistId = state.externalPlaylistId();
        this.title = state.title();
        this.sourcePlatform = state.sourcePlatform();
        this.curator = state.curator();
        this.highlight = state.highlight();
        this.coverImageUrl = state.coverImageUrl();
        this.platformExternalUrl = state.platformExternalUrl();
        this.platformUri = state.platformUri();
        this.lastSyncedAt = state.lastSyncedAt();
    }

    public Long getUserPlaylistId() {
        return userPlaylistId;
    }

    public String getUserId() {
        return userId;
    }

    public String getPlaylistId() {
        return playlistId;
    }

    public String getExternalPlaylistId() {
        return externalPlaylistId;
    }

    public String getTitle() {
        return title;
    }

    public String getSourcePlatform() {
        return sourcePlatform;
    }

    public String getCurator() {
        return curator;
    }

    public String getHighlight() {
        return highlight;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public String getPlatformExternalUrl() {
        return platformExternalUrl;
    }

    public String getPlatformUri() {
        return platformUri;
    }

    public Instant getLastSyncedAt() {
        return lastSyncedAt;
    }
}
