package io.myforevermusic.api.modules.platform.infrastructure.persistence;

import io.myforevermusic.api.modules.platform.application.PlatformAccountCredential;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "platform_account_credential")
public class PlatformAccountCredentialEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "credential_id", nullable = false)
    private Long credentialId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "platform_id", nullable = false, length = 50)
    private String platformId;

    @Column(name = "authorization_mode", nullable = false, length = 50)
    private String authorizationMode;

    @Column(name = "external_user_id", length = 150)
    private String externalUserId;

    @Column(name = "external_account_label", length = 200)
    private String externalAccountLabel;

    @Column(name = "access_token", nullable = false, length = 500)
    private String accessToken;

    @Column(name = "refresh_token", length = 500)
    private String refreshToken;

    @Column(name = "token_type", nullable = false, length = 30)
    private String tokenType;

    @Column(name = "scope_summary", length = 300)
    private String scopeSummary;

    @Column(name = "access_token_expires_at")
    private Instant accessTokenExpiresAt;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PlatformAccountCredentialEntity() {
    }

    public PlatformAccountCredentialEntity(PlatformAccountCredential credential) {
        this.userId = credential.userId();
        this.platformId = credential.platformId();
        this.authorizationMode = credential.authorizationMode();
        this.externalUserId = credential.externalUserId();
        this.externalAccountLabel = credential.externalAccountLabel();
        this.accessToken = credential.accessToken();
        this.refreshToken = credential.refreshToken();
        this.tokenType = credential.tokenType();
        this.scopeSummary = credential.scopeSummary();
        this.accessTokenExpiresAt = credential.accessTokenExpiresAt();
        this.issuedAt = credential.issuedAt();
        this.updatedAt = credential.updatedAt();
    }

    public void apply(PlatformAccountCredential credential) {
        this.authorizationMode = credential.authorizationMode();
        this.externalUserId = credential.externalUserId();
        this.externalAccountLabel = credential.externalAccountLabel();
        this.accessToken = credential.accessToken();
        this.refreshToken = credential.refreshToken();
        this.tokenType = credential.tokenType();
        this.scopeSummary = credential.scopeSummary();
        this.accessTokenExpiresAt = credential.accessTokenExpiresAt();
        this.issuedAt = credential.issuedAt();
        this.updatedAt = credential.updatedAt();
    }

    public PlatformAccountCredential toCredential() {
        return new PlatformAccountCredential(
            userId,
            platformId,
            authorizationMode,
            externalUserId,
            externalAccountLabel,
            accessToken,
            refreshToken,
            tokenType,
            scopeSummary,
            accessTokenExpiresAt,
            issuedAt,
            updatedAt
        );
    }
}
