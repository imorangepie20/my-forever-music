package io.myforevermusic.api.modules.auth.infrastructure.persistence;

import io.myforevermusic.api.modules.auth.application.AuthRegisteredAccount;
import io.myforevermusic.api.modules.auth.application.AuthAuthenticationAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "auth_user_account")
public class AuthUserAccountEntity {

    @Id
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "normalized_email", nullable = false, length = 320)
    private String normalizedEmail;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "password_hash", nullable = false, length = 120)
    private String passwordHash;

    @Column(name = "preferred_platform_id", nullable = false, length = 50)
    private String preferredPlatformId;

    @Column(name = "last_fm_username", length = 120)
    private String lastFmUsername;

    @Column(name = "last_fm_connected_at")
    private Instant lastFmConnectedAt;

    @Column(name = "marketing_opt_in", nullable = false)
    private boolean marketingOptIn;

    @Column(name = "onboarding_stage", nullable = false, length = 50)
    private String onboardingStage;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    @Column(name = "accepted_terms_at", nullable = false)
    private Instant acceptedTermsAt;

    @Column(name = "accepted_privacy_policy_at", nullable = false)
    private Instant acceptedPrivacyPolicyAt;

    protected AuthUserAccountEntity() {
    }

    public AuthUserAccountEntity(
        String userId,
        String email,
        String normalizedEmail,
        String displayName,
        String passwordHash,
        String preferredPlatformId,
        String lastFmUsername,
        Instant lastFmConnectedAt,
        boolean marketingOptIn,
        String onboardingStage,
        Instant registeredAt,
        Instant acceptedTermsAt,
        Instant acceptedPrivacyPolicyAt
    ) {
        this.userId = userId;
        this.email = email;
        this.normalizedEmail = normalizedEmail;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.preferredPlatformId = preferredPlatformId;
        this.lastFmUsername = lastFmUsername;
        this.lastFmConnectedAt = lastFmConnectedAt;
        this.marketingOptIn = marketingOptIn;
        this.onboardingStage = onboardingStage;
        this.registeredAt = registeredAt;
        this.acceptedTermsAt = acceptedTermsAt;
        this.acceptedPrivacyPolicyAt = acceptedPrivacyPolicyAt;
    }

    public AuthRegisteredAccount toRegisteredAccount() {
        return new AuthRegisteredAccount(
            userId,
            email,
            normalizedEmail,
            displayName,
            preferredPlatformId,
            lastFmUsername,
            lastFmConnectedAt,
            marketingOptIn,
            onboardingStage,
            registeredAt,
            acceptedTermsAt,
            acceptedPrivacyPolicyAt
        );
    }

    public AuthAuthenticationAccount toAuthenticationAccount() {
        return new AuthAuthenticationAccount(
            toRegisteredAccount(),
            passwordHash
        );
    }

    public void setLastFmProfile(String lastFmUsername, Instant lastFmConnectedAt) {
        this.lastFmUsername = lastFmUsername;
        this.lastFmConnectedAt = lastFmConnectedAt;
    }
}
