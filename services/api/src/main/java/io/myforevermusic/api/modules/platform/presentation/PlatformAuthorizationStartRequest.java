package io.myforevermusic.api.modules.platform.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PlatformAuthorizationStartRequest(
    @NotBlank(message = "User id is required.")
    String userId,

    @NotBlank(message = "Platform id is required.")
    String platformId
) {
}
