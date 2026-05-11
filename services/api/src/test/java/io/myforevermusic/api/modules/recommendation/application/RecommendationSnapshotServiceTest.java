package io.myforevermusic.api.modules.recommendation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.myforevermusic.api.modules.gms.presentation.GmsRecommendationPreviewRequest;
import io.myforevermusic.api.modules.gms.presentation.GmsRecommendationPreviewResponse;
import io.myforevermusic.api.modules.recommendation.infrastructure.local.InMemoryRecommendationSnapshotStore;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecommendationSnapshotServiceTest {

    @Test
    void shouldRecordGmsPreviewRecommendationSnapshots() {
        InMemoryRecommendationSnapshotStore snapshotStore = new InMemoryRecommendationSnapshotStore();
        RecommendationSnapshotService service = new RecommendationSnapshotService(
            snapshotStore,
            Clock.fixed(Instant.parse("2026-05-11T00:00:00Z"), ZoneOffset.UTC)
        );

        List<RecommendationSnapshotStore.StoredSnapshot> snapshots = service.recordGmsPreview(
            new GmsRecommendationPreviewRequest(
                "request-001",
                "user-001",
                "playlist-001",
                "gms",
                "upbeat",
                4,
                2,
                5,
                List.of("track-seed"),
                List.of("Neon Bloom"),
                List.of("synth-pop"),
                true
            ),
            new GmsRecommendationPreviewResponse(
                "recommendation-001",
                Instant.parse("2026-05-11T00:00:00Z"),
                "ai",
                "ok",
                null,
                null,
                List.of(new GmsRecommendationPreviewResponse.RecommendationItem(
                    1,
                    "track-001",
                    "Midnight Receiver",
                    "Neon Bloom",
                    "spotify",
                    "playlist-001",
                    "Night Drive Archive",
                    "Signal Bloom",
                    null,
                    null,
                    "spotify:track:spotify-track-001",
                    null,
                    "spotify-track-001",
                    "spotify-track-001",
                    218000,
                    0.84,
                    "gms",
                    4,
                    "Strong affinity."
                )),
                List.of()
            )
        );

        assertThat(snapshots).hasSize(1);
        assertThat(snapshots.getFirst().recommendationId()).isEqualTo("recommendation-001");
        assertThat(snapshots.getFirst().requestId()).isEqualTo("request-001");
        assertThat(snapshots.getFirst().candidateTrackId()).isEqualTo("track-001");
        assertThat(snapshots.getFirst().modelVersion()).isEqualTo("gms-baseline-v1");
        assertThat(snapshots.getFirst().affinityScore()).isEqualTo(0.84);
        assertThat(snapshots.getFirst().coherenceScore()).isEqualTo(0.9);
        assertThat(snapshots.getFirst().diversityScore()).isEqualTo(1.0);
        assertThat(snapshots.getFirst().redundancyPenalty()).isEqualTo(0.0);
        assertThat(snapshots.getFirst().confidenceScore()).isCloseTo(0.95, within(0.0001));
        assertThat(snapshotStore.findRecentByUserId("user-001", 10)).hasSize(1);
    }
}
