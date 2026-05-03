package io.myforevermusic.api.modules.platform.infrastructure.persistence;

import io.myforevermusic.api.modules.platform.application.PlatformConnectionState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "platform_account_connection")
public class PlatformAccountConnectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "connection_id", nullable = false)
    private Long connectionId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "platform_id", nullable = false, length = 50)
    private String platformId;

    @Column(name = "connected", nullable = false)
    private boolean connected;

    @Column(name = "connection_status", nullable = false, length = 50)
    private String connectionStatus;

    @Column(name = "connection_mode", nullable = false, length = 50)
    private String connectionMode;

    @Column(name = "external_account_label", length = 200)
    private String externalAccountLabel;

    @Column(name = "scope_summary", length = 200)
    private String scopeSummary;

    @Column(name = "sync_ready", nullable = false)
    private boolean syncReady;

    @Column(name = "connected_at")
    private Instant connectedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PlatformAccountConnectionEntity() {
    }

    public PlatformAccountConnectionEntity(
        String userId,
        String platformId,
        boolean connected,
        String connectionStatus,
        String connectionMode,
        String externalAccountLabel,
        String scopeSummary,
        boolean syncReady,
        Instant connectedAt,
        Instant updatedAt
    ) {
        this.userId = userId;
        this.platformId = platformId;
        this.connected = connected;
        this.connectionStatus = connectionStatus;
        this.connectionMode = connectionMode;
        this.externalAccountLabel = externalAccountLabel;
        this.scopeSummary = scopeSummary;
        this.syncReady = syncReady;
        this.connectedAt = connectedAt;
        this.updatedAt = updatedAt;
    }

    public String getUserId() {
        return userId;
    }

    public String getPlatformId() {
        return platformId;
    }

    public void markConnected(
        String connectionMode,
        String externalAccountLabel,
        String scopeSummary,
        boolean syncReady,
        Instant connectedAt,
        Instant updatedAt
    ) {
        this.connected = true;
        this.connectionStatus = "connected";
        this.connectionMode = connectionMode;
        this.externalAccountLabel = externalAccountLabel;
        this.scopeSummary = scopeSummary;
        this.syncReady = syncReady;
        this.connectedAt = connectedAt;
        this.updatedAt = updatedAt;
    }

    public void markDisconnected(Instant updatedAt) {
        this.connected = false;
        this.connectionStatus = "not_connected";
        this.syncReady = false;
        this.updatedAt = updatedAt;
    }

    public PlatformConnectionState toState() {
        return new PlatformConnectionState(
            userId,
            platformId,
            connected,
            connectionStatus,
            connectionMode,
            externalAccountLabel,
            scopeSummary,
            syncReady,
            connectedAt,
            updatedAt
        );
    }
}
