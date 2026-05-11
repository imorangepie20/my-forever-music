package io.myforevermusic.api.modules.recommendation.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.myforevermusic.api.modules.recommendation.application.UserMusicEventStore;
import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UserMusicEventResponse(
    String service,
    String status,
    Instant processedAt,
    Event event,
    String nextStepMessage
) {

    public static UserMusicEventResponse from(UserMusicEventStore.StoredEvent storedEvent, Instant processedAt) {
        return new UserMusicEventResponse(
            "user-music-event",
            "recorded",
            processedAt,
            Event.from(storedEvent),
            "Event is now available as a recommendation learning signal."
        );
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Event(
        Long eventId,
        String userId,
        String eventType,
        Double eventWeight,
        String sourceSpace,
        String sourcePlatform,
        String playbackPlatformId,
        String itemId,
        String itemKind,
        String trackId,
        String playlistId,
        String externalTrackId,
        String platformUri,
        String title,
        String artistName,
        String albumTitle,
        String isrc,
        Integer durationMs,
        Integer positionMs,
        Double playRatio,
        String recommendationId,
        Double metadataConfidence,
        Instant occurredAt,
        Instant receivedAt
    ) {

        static Event from(UserMusicEventStore.StoredEvent storedEvent) {
            return new Event(
                storedEvent.eventId(),
                storedEvent.userId(),
                storedEvent.eventType(),
                storedEvent.eventWeight(),
                storedEvent.sourceSpace(),
                storedEvent.sourcePlatform(),
                storedEvent.playbackPlatformId(),
                storedEvent.itemId(),
                storedEvent.itemKind(),
                storedEvent.trackId(),
                storedEvent.playlistId(),
                storedEvent.externalTrackId(),
                storedEvent.platformUri(),
                storedEvent.title(),
                storedEvent.artistName(),
                storedEvent.albumTitle(),
                storedEvent.isrc(),
                storedEvent.durationMs(),
                storedEvent.positionMs(),
                storedEvent.playRatio(),
                storedEvent.recommendationId(),
                storedEvent.metadataConfidence(),
                storedEvent.occurredAt(),
                storedEvent.receivedAt()
            );
        }
    }
}
