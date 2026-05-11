package io.myforevermusic.api.modules.recommendation.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.myforevermusic.api.modules.recommendation.infrastructure.local.InMemoryRecommendationSnapshotStore;
import io.myforevermusic.api.modules.recommendation.infrastructure.local.InMemoryUserMusicEventStore;
import io.myforevermusic.api.modules.recommendation.presentation.RecommendationDatasetExportResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RecommendationDatasetExportServiceTest {

    @Test
    void shouldExportUserEventsAndRecommendationSnapshotsAsSequence() {
        InMemoryUserMusicEventStore eventStore = new InMemoryUserMusicEventStore();
        InMemoryRecommendationSnapshotStore snapshotStore = new InMemoryRecommendationSnapshotStore();
        RecommendationDatasetExportService service = new RecommendationDatasetExportService(
            eventStore,
            snapshotStore,
            Clock.fixed(Instant.parse("2026-05-11T03:00:00Z"), ZoneOffset.UTC)
        );

        eventStore.save(new UserMusicEventStore.EventDraft(
            "user-001",
            "play_started",
            0.2,
            "player",
            "spotify",
            "spotify",
            "track-001",
            "track",
            "track-001",
            "playlist-001",
            "spotify-track-001",
            "spotify:track:001",
            "Midnight Receiver",
            "Neon Bloom",
            "Signal Bloom",
            "USRC17607839",
            210000,
            0,
            null,
            null,
            0.95,
            Instant.parse("2026-05-11T01:00:00Z")
        ));
        snapshotStore.saveAll(java.util.List.of(new RecommendationSnapshotStore.SnapshotDraft(
            "recommendation-001",
            "request-001",
            "user-001",
            "track-002",
            "playlist-002",
            "Signal Run",
            "Neon Bloom",
            "gms",
            "tidal",
            "gms-baseline-v1",
            "audio-track-002",
            0.88,
            0.75,
            0.82,
            0.93,
            0.0,
            0.95,
            1,
            "Strong affinity.",
            Instant.parse("2026-05-11T02:00:00Z")
        )));

        RecommendationDatasetExportResponse response = service.exportUserSequence("user-001", 10, 10);

        assertThat(response.generatedAt()).isEqualTo(Instant.parse("2026-05-11T03:00:00Z"));
        assertThat(response.summary().eventCount()).isEqualTo(1);
        assertThat(response.summary().recommendationSnapshotCount()).isEqualTo(1);
        assertThat(response.sequence()).hasSize(2);
        assertThat(response.sequence().get(0).token()).isEqualTo("event:play_started:track-001");
        assertThat(response.sequence().get(1).token()).isEqualTo("recommendation:gms-baseline-v1:track-002");
    }
}
