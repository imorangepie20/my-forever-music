package io.myforevermusic.api.modules.recommendation.application;

import io.myforevermusic.api.modules.recommendation.presentation.UserMusicEventRequest;
import io.myforevermusic.api.modules.recommendation.presentation.UserMusicEventResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserMusicEventService {

    private static final String DEFAULT_SOURCE_SPACE = "player";
    private static final Set<String> SUPPORTED_EVENT_TYPES = Set.of(
        "play_started",
        "play_paused",
        "play_resumed",
        "play_completed",
        "skip_next",
        "skip_previous",
        "replay",
        "track_saved",
        "added_to_playlist",
        "recommendation_liked",
        "recommendation_rejected",
        "ignored_recommendation",
        "stopped_midway"
    );

    private final UserMusicEventStore eventStore;
    private final EventSignalWeights eventSignalWeights;
    private final Clock clock;

    @Autowired
    public UserMusicEventService(UserMusicEventStore eventStore, EventSignalWeights eventSignalWeights) {
        this(eventStore, eventSignalWeights, Clock.systemUTC());
    }

    UserMusicEventService(UserMusicEventStore eventStore, EventSignalWeights eventSignalWeights, Clock clock) {
        this.eventStore = eventStore;
        this.eventSignalWeights = eventSignalWeights;
        this.clock = clock;
    }

    public UserMusicEventResponse recordEvent(UserMusicEventRequest request) {
        String eventType = normalizeEventType(request.eventType());
        String sourceSpace = normalizeOptional(request.sourceSpace());
        if (sourceSpace == null) {
            sourceSpace = DEFAULT_SOURCE_SPACE;
        }
        Instant occurredAt = request.occurredAt() == null ? Instant.now(clock) : request.occurredAt();

        UserMusicEventStore.StoredEvent storedEvent = eventStore.save(
            new UserMusicEventStore.EventDraft(
                request.userId(),
                eventType,
                eventSignalWeights.weightFor(eventType),
                sourceSpace,
                normalizeOptional(request.sourcePlatform()),
                normalizeOptional(request.playbackPlatformId()),
                normalizeOptional(request.itemId()),
                normalizeOptional(request.itemKind()),
                normalizeOptional(request.trackId()),
                normalizeOptional(request.playlistId()),
                normalizeOptional(request.externalTrackId()),
                normalizeOptional(request.platformUri()),
                normalizeOptional(request.title()),
                normalizeOptional(request.artistName()),
                normalizeOptional(request.albumTitle()),
                normalizeOptional(request.isrc()),
                request.durationMs(),
                request.positionMs(),
                clampRatio(request.playRatio()),
                normalizeOptional(request.recommendationId()),
                clampRatio(request.metadataConfidence()),
                occurredAt
            )
        );

        return UserMusicEventResponse.from(storedEvent, Instant.now(clock));
    }

    private String normalizeEventType(String eventType) {
        String normalized = eventType == null ? "" : eventType.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_EVENT_TYPES.contains(normalized)) {
            throw new IllegalArgumentException(
                "User music event type must be one of: %s.".formatted(String.join(", ", SUPPORTED_EVENT_TYPES))
            );
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private Double clampRatio(Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            return null;
        }
        return Math.min(1.0, Math.max(0.0, value));
    }
}
