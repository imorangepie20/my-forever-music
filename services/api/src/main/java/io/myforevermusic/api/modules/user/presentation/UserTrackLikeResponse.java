package io.myforevermusic.api.modules.user.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UserTrackLikeResponse(
    boolean liked,
    String sourcePlatform,
    String externalTrackId,
    Instant likedAt
) {
}
