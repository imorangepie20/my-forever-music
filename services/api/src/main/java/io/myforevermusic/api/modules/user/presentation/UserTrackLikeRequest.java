package io.myforevermusic.api.modules.user.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UserTrackLikeRequest(
    @NotBlank String userId,
    @NotBlank String sourcePlatform,
    @NotBlank String externalTrackId,
    String title,
    String artistName,
    String albumTitle,
    String imageUrl,
    String spotifyTrackId,
    String platformExternalUrl
) {
}
