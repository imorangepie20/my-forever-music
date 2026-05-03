package io.myforevermusic.api.modules.platform.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record LastFmScrobbleSyncRequest(
    @NotBlank(message = "user_id is required.")
    String userId,
    @Min(value = 10, message = "limit must be at least 10.")
    @Max(value = 100, message = "limit must not exceed 100.")
    Integer limit
) {

    public LastFmScrobbleSyncRequest {
        limit = limit == null ? 40 : limit;
    }
}
