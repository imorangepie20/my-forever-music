package io.myforevermusic.api.modules.auth.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AuthLoginResponse(
    String service,
    String status,
    Instant authenticatedAt,
    AuthRegistrationResponse.RegisteredUser user,
    AuthRegistrationResponse.OnboardingState onboarding
) {
}
