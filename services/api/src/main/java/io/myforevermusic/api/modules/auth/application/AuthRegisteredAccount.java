package io.myforevermusic.api.modules.auth.application;

import java.time.Instant;

public record AuthRegisteredAccount(
    String userId,
    String email,
    String normalizedEmail,
    String displayName,
    String preferredPlatformId,
    boolean marketingOptIn,
    String onboardingStage,
    Instant registeredAt,
    Instant acceptedTermsAt,
    Instant acceptedPrivacyPolicyAt
) {
}
