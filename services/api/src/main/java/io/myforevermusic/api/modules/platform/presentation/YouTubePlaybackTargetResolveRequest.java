package io.myforevermusic.api.modules.platform.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record YouTubePlaybackTargetResolveRequest(
    @NotBlank String userId,
    @NotBlank String title,
    @NotBlank String artistName,
    String sourcePlatform,
    String externalTrackId,
    String platformUri,
    String spotifyTrackId,
    String tidalTrackId,
    String isrc,
    Integer durationMs,
    List<String> excludedVideoIds
) {
}
