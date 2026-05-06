package io.myforevermusic.api.modules.pms.infrastructure.persistence;

import io.myforevermusic.api.modules.pms.application.PmsPersonalPlaylistStore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "pms_personal_playlist")
public class PmsPersonalPlaylistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "personal_playlist_id", nullable = false)
    private Long personalPlaylistId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "playlist_id", nullable = false, length = 160)
    private String playlistId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PmsPersonalPlaylistEntity() {
    }

    public PmsPersonalPlaylistEntity(PmsPersonalPlaylistStore.CreatePlaylistDraft draft) {
        Instant now = Instant.now();
        this.userId = draft.userId();
        this.playlistId = draft.playlistId();
        this.title = draft.title();
        this.description = draft.description();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void markUpdated(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getPersonalPlaylistId() {
        return personalPlaylistId;
    }

    public String getUserId() {
        return userId;
    }

    public String getPlaylistId() {
        return playlistId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
