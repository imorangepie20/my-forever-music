package io.myforevermusic.api.modules.pms.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PmsPersonalPlaylistCreateRequest(
    @NotBlank(message = "user_id is required.")
    @Size(max = 100, message = "user_id must be 100 characters or fewer.")
    String userId,

    @NotBlank(message = "title is required.")
    @Size(max = 200, message = "title must be 200 characters or fewer.")
    String title,

    @Size(max = 1000, message = "description must be 1000 characters or fewer.")
    String description
) {
}
