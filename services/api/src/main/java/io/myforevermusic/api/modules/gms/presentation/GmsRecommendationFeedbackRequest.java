package io.myforevermusic.api.modules.gms.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GmsRecommendationFeedbackRequest(
    @NotBlank(message = "user_id is required.")
    @Size(max = 100, message = "user_id must be 100 characters or fewer.")
    String userId,

    @Size(max = 160, message = "request_id must be 160 characters or fewer.")
    String requestId,

    @Size(max = 160, message = "playlist_id must be 160 characters or fewer.")
    String playlistId,

    @NotBlank(message = "track_id is required.")
    @Size(max = 160, message = "track_id must be 160 characters or fewer.")
    String trackId,

    @NotBlank(message = "feedback_type is required.")
    @Size(max = 30, message = "feedback_type must be 30 characters or fewer.")
    String feedbackType,

    Integer score,

    @Size(max = 50, message = "source_space must be 50 characters or fewer.")
    String sourceSpace,

    @Size(max = 1000, message = "reason must be 1000 characters or fewer.")
    String reason
) {
}
