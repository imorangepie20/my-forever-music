package io.myforevermusic.api.modules.recommendation.application;

import java.time.Instant;
import java.util.List;

public interface UserMusicEventStore {

    StoredEvent save(EventDraft draft);

    List<StoredEvent> findRecentByUserId(String userId, int limit);

    List<String> findActiveUserIds(Instant since, int limit);

    long countEventsByUserIdAfter(String userId, Instant since);

    record EventDraft(
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
        Instant occurredAt
    ) {
    }

    record StoredEvent(
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
    }
}
