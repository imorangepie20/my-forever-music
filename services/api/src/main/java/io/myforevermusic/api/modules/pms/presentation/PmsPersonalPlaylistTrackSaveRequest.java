package io.myforevermusic.api.modules.pms.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PmsPersonalPlaylistTrackSaveRequest(
    @NotBlank(message = "user_id is required.")
    @Size(max = 100, message = "user_id must be 100 characters or fewer.")
    String userId,

    @Size(max = 160, message = "target_playlist_id must be 160 characters or fewer.")
    String targetPlaylistId,

    @Size(max = 200, message = "target_playlist_title must be 200 characters or fewer.")
    String targetPlaylistTitle,

    @NotBlank(message = "track_id is required.")
    @Size(max = 160, message = "track_id must be 160 characters or fewer.")
    String trackId,

    @Size(max = 80, message = "source_context must be 80 characters or fewer.")
    String sourceContext
) {
}
