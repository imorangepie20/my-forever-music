package io.myforevermusic.api.modules.platform.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SpotifyPlaybackTargetResolveResponse(
    String service,
    String status,
    Instant generatedAt,
    String userId,
    String sourcePlatform,
    String sourceTrackId,
    String spotifyTrackId,
    String spotifyUri,
    String title,
    String artistName,
    String albumTitle,
    String albumImageUrl,
    String platformExternalUrl,
    String previewUrl,
    String isrc,
    Integer durationMs,
    String matchReason,
    int matchScore
) {
}
