package io.myforevermusic.api.modules.recommendation.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.myforevermusic.api.modules.recommendation.application.RecommendationSnapshotStore;
import io.myforevermusic.api.modules.recommendation.application.UserMusicEventStore;
import java.time.Instant;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RecommendationDatasetExportResponse(
    String userId,
    Instant generatedAt,
    Integer eventLimit,
    Integer snapshotLimit,
    Summary summary,
    List<EventItem> events,
    List<RecommendationSnapshotItem> recommendationSnapshots,
    List<SequenceItem> sequence
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Summary(
        Integer eventCount,
        Integer recommendationSnapshotCount,
        Integer sequenceItemCount,
        String datasetVersion,
        String datasetFingerprint
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EventItem(
        Long eventId,
        String eventType,
        Double eventWeight,
        String sourceSpace,
        String sourcePlatform,
        String trackId,
        String playlistId,
        String itemId,
        String itemKind,
        String title,
        String artistName,
        String recommendationId,
        Double metadataConfidence,
        Instant occurredAt
    ) {
        public static EventItem from(UserMusicEventStore.StoredEvent event) {
            return new EventItem(
                event.eventId(),
                event.eventType(),
                event.eventWeight(),
                event.sourceSpace(),
                event.sourcePlatform(),
                event.trackId(),
                event.playlistId(),
                event.itemId(),
                event.itemKind(),
                event.title(),
                event.artistName(),
                event.recommendationId(),
                event.metadataConfidence(),
                event.occurredAt()
            );
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record RecommendationSnapshotItem(
        Long snapshotId,
        String recommendationId,
        String requestId,
        String candidateTrackId,
        String candidatePlaylistId,
        String candidateTitle,
        String candidateArtistName,
        String sourceSpace,
        String sourcePlatform,
        String modelVersion,
        String featureSnapshotId,
        Double affinityScore,
        Double noveltyScore,
        Double coherenceScore,
        Double diversityScore,
        Double redundancyPenalty,
        Double confidenceScore,
        Integer rank,
        Instant createdAt
    ) {
        public static RecommendationSnapshotItem from(RecommendationSnapshotStore.StoredSnapshot snapshot) {
            return new RecommendationSnapshotItem(
                snapshot.snapshotId(),
                snapshot.recommendationId(),
                snapshot.requestId(),
                snapshot.candidateTrackId(),
                snapshot.candidatePlaylistId(),
                snapshot.candidateTitle(),
                snapshot.candidateArtistName(),
                snapshot.sourceSpace(),
                snapshot.sourcePlatform(),
                snapshot.modelVersion(),
                snapshot.featureSnapshotId(),
                snapshot.affinityScore(),
                snapshot.noveltyScore(),
                snapshot.coherenceScore(),
                snapshot.diversityScore(),
                snapshot.redundancyPenalty(),
                snapshot.confidenceScore(),
                snapshot.rank(),
                snapshot.createdAt()
            );
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SequenceItem(
        String itemType,
        Long sourceId,
        String token,
        String trackId,
        String playlistId,
        String recommendationId,
        Double weight,
        Instant occurredAt
    ) {
        public static SequenceItem fromEvent(UserMusicEventStore.StoredEvent event) {
            return new SequenceItem(
                "event",
                event.eventId(),
                "event:%s:%s".formatted(event.eventType(), safeTokenValue(event.trackId())),
                event.trackId(),
                event.playlistId(),
                event.recommendationId(),
                event.eventWeight(),
                event.occurredAt()
            );
        }

        public static SequenceItem fromSnapshot(RecommendationSnapshotStore.StoredSnapshot snapshot) {
            return new SequenceItem(
                "recommendation_snapshot",
                snapshot.snapshotId(),
                "recommendation:%s:%s".formatted(snapshot.modelVersion(), safeTokenValue(snapshot.candidateTrackId())),
                snapshot.candidateTrackId(),
                snapshot.candidatePlaylistId(),
                snapshot.recommendationId(),
                snapshot.affinityScore(),
                snapshot.createdAt()
            );
        }

        private static String safeTokenValue(String value) {
            return value == null || value.isBlank() ? "unknown" : value.trim();
        }
    }
}
