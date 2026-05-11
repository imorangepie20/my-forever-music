package io.myforevermusic.api.modules.recommendation.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UserMusicEventRequest(
    @NotBlank @Size(max = 100) String userId,
    @NotBlank @Size(max = 50) String eventType,
    @Size(max = 50) String sourceSpace,
    @Size(max = 50) String sourcePlatform,
    @Size(max = 50) String playbackPlatformId,
    @Size(max = 200) String itemId,
    @Size(max = 30) String itemKind,
    @Size(max = 200) String trackId,
    @Size(max = 200) String playlistId,
    @Size(max = 200) String externalTrackId,
    @Size(max = 500) String platformUri,
    @Size(max = 500) String title,
    @Size(max = 500) String artistName,
    @Size(max = 500) String albumTitle,
    @Size(max = 50) String isrc,
    @Min(0) Integer durationMs,
    @Min(0) Integer positionMs,
    @DecimalMin("0.0") @DecimalMax("1.0") Double playRatio,
    @Size(max = 160) String recommendationId,
    @DecimalMin("0.0") @DecimalMax("1.0") Double metadataConfidence,
    Instant occurredAt
) {
}
