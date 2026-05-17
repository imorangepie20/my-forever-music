package io.myforevermusic.api.modules.user.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UserTrackLikeListResponse(
    long totalCount,
    List<Entry> items
) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Entry(
        String sourcePlatform,
        String externalTrackId,
        String spotifyTrackId,
        String title,
        String artistName,
        String albumTitle,
        String imageUrl,
        String platformExternalUrl,
        Instant likedAt
    ) {
    }
}
