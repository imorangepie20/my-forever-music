package io.myforevermusic.api.modules.platform.presentation;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record TidalDeviceAuthorizationPollRequest(
    @JsonProperty("user_id") @NotBlank String userId,
    @JsonProperty("device_code") @NotBlank String deviceCode
) {
}
