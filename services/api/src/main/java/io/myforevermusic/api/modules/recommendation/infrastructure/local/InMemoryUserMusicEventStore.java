package io.myforevermusic.api.modules.recommendation.infrastructure.local;

import io.myforevermusic.api.modules.recommendation.application.UserMusicEventStore;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class InMemoryUserMusicEventStore implements UserMusicEventStore {

    private final AtomicLong sequence = new AtomicLong(1);
    private final Map<Long, StoredEvent> eventById = new ConcurrentHashMap<>();

    @Override
    public StoredEvent save(EventDraft draft) {
        long eventId = sequence.getAndIncrement();
        StoredEvent storedEvent = new StoredEvent(
            eventId,
            draft.userId(),
            draft.eventType(),
            draft.eventWeight(),
            draft.sourceSpace(),
            draft.sourcePlatform(),
            draft.playbackPlatformId(),
            draft.itemId(),
            draft.itemKind(),
            draft.trackId(),
            draft.playlistId(),
            draft.externalTrackId(),
            draft.platformUri(),
            draft.title(),
            draft.artistName(),
            draft.albumTitle(),
            draft.isrc(),
            draft.durationMs(),
            draft.positionMs(),
            draft.playRatio(),
            draft.recommendationId(),
            draft.metadataConfidence(),
            draft.occurredAt(),
            Instant.now()
        );
        eventById.put(eventId, storedEvent);
        return storedEvent;
    }

    @Override
    public List<StoredEvent> findRecentByUserId(String userId, int limit) {
        return eventById.values().stream()
            .filter(event -> event.userId().equals(userId))
            .sorted(Comparator.comparing(StoredEvent::occurredAt).reversed())
            .limit(Math.max(0, limit))
            .toList();
    }
}
