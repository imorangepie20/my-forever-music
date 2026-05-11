package io.myforevermusic.api.modules.platform.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SpotifyPlaybackTargetResolveRequest(
    @NotBlank String userId,
    @NotBlank String title,
    @NotBlank String artistName,
    String sourcePlatform,
    String externalTrackId,
    String platformUri,
    String tidalTrackId,
    String isrc,
    Integer durationMs
) {
}
