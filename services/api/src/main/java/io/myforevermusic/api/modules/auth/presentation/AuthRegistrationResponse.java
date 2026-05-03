package io.myforevermusic.api.modules.auth.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AuthRegistrationResponse(
    String service,
    String status,
    Instant registeredAt,
    RegisteredUser user,
    OnboardingState onboarding
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record RegisteredUser(
        String userId,
        String email,
        String displayName,
        boolean emailVerified
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record OnboardingState(
        String stage,
        String preferredPlatformId,
        boolean platformConnectionRequired,
        String nextStepPath,
        String nextStepMessage
    ) {
    }
}
