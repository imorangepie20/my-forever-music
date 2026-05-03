package io.myforevermusic.api.modules.auth.application;

import java.time.Instant;

public record AuthRegistrationDraft(
    String userId,
    String email,
    String normalizedEmail,
    String displayName,
    String passwordHash,
    String preferredPlatformId,
    boolean marketingOptIn,
    String onboardingStage,
    Instant registeredAt,
    Instant acceptedTermsAt,
    Instant acceptedPrivacyPolicyAt
) {
}
