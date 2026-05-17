package io.myforevermusic.api.modules.platform.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record YouTubePlaybackTargetResolveResponse(
    String service,
    String status,
    Instant generatedAt,
    String userId,
    String sourcePlatform,
    String sourceTrackId,
    String youtubeVideoId,
    String youtubeUrl,
    String title,
    String channelTitle,
    String thumbnailUrl,
    Integer durationMs,
    String matchReason,
    int matchScore,
    int candidateCount
) {
}
