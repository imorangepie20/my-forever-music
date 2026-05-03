package io.myforevermusic.api.modules.pms.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PmsPlaylistImportRequest(
    @NotBlank(message = "user_id is required.")
    String userId,
    @NotBlank(message = "platform_id is required.")
    String platformId,
    @NotEmpty(message = "external_playlist_ids must contain at least one playlist.")
    List<String> externalPlaylistIds
) {
}
