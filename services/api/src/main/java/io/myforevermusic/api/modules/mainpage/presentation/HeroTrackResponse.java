package io.myforevermusic.api.modules.mainpage.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record HeroTrackResponse(
    String externalTrackId,
    String sourcePlatform,
    String spotifyTrackId,
    String title,
    String artistName,
    String albumTitle,
    String imageUrl,
    String previewUrl,
    String platformExternalUrl,
    Integer durationMs,
    String sourceLabel
) {
}
