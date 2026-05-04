package io.myforevermusic.api.modules.platform.infrastructure.persistence;

import io.myforevermusic.api.modules.platform.application.PlatformAuthorizationSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "platform_authorization_session")
public class PlatformAuthorizationSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "state", nullable = false, length = 120, unique = true)
    private String state;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "platform_id", nullable = false, length = 50)
    private String platformId;

    @Column(name = "platform_display_name", nullable = false, length = 120)
    private String platformDisplayName;

    @Column(name = "authorization_mode", nullable = false, length = 50)
    private String authorizationMode;

    @Column(name = "authorization_channel", nullable = false, length = 50)
    private String authorizationChannel;

    @Column(name = "requested_scopes", nullable = false, length = 300)
    private String requestedScopes;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "approval_code", length = 80)
    private String approvalCode;

    @Column(name = "external_authorization_url", length = 1000)
    private String externalAuthorizationUrl;

    @Column(name = "redirect_uri", length = 500)
    private String redirectUri;

    @Column(name = "pkce_code_verifier", length = 150)
    private String pkceCodeVerifier;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected PlatformAuthorizationSessionEntity() {
    }

    public PlatformAuthorizationSessionEntity(
        String state,
        String userId,
        String platformId,
        String platformDisplayName,
        String authorizationMode,
        String authorizationChannel,
        String requestedScopes,
        String status,
        String approvalCode,
        String externalAuthorizationUrl,
        String redirectUri,
        String pkceCodeVerifier,
        Instant expiresAt,
        Instant createdAt,
        Instant completedAt
    ) {
        this.state = state;
        this.userId = userId;
        this.platformId = platformId;
        this.platformDisplayName = platformDisplayName;
        this.authorizationMode = authorizationMode;
        this.authorizationChannel = authorizationChannel;
        this.requestedScopes = requestedScopes;
        this.status = status;
        this.approvalCode = approvalCode;
        this.externalAuthorizationUrl = externalAuthorizationUrl;
        this.redirectUri = redirectUri;
        this.pkceCodeVerifier = pkceCodeVerifier;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public void markCompleted(Instant completedAt) {
        this.status = "completed";
        this.completedAt = completedAt;
    }

    public PlatformAuthorizationSession toSession() {
        List<String> scopes = requestedScopes.isBlank()
            ? List.of()
            : List.of(requestedScopes.split(",\\s*"));

        return new PlatformAuthorizationSession(
            state,
            userId,
            platformId,
            platformDisplayName,
            authorizationMode,
            authorizationChannel,
            scopes,
            status,
            approvalCode,
            externalAuthorizationUrl,
            redirectUri,
            pkceCodeVerifier,
            expiresAt,
            createdAt,
            completedAt
        );
    }
}
