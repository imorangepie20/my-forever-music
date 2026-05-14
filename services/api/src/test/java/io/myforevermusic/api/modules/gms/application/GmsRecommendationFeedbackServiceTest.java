package io.myforevermusic.api.modules.gms.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.myforevermusic.api.modules.gms.infrastructure.local.InMemoryGmsRecommendationFeedbackStore;
import io.myforevermusic.api.modules.gms.presentation.GmsRecommendationFeedbackRequest;
import io.myforevermusic.api.modules.gms.presentation.GmsRecommendationFeedbackResponse;
import io.myforevermusic.api.modules.pms.application.PmsUserLibraryStore;
import io.myforevermusic.api.modules.pms.infrastructure.local.InMemoryPmsUserLibraryStore;
import io.myforevermusic.api.modules.pms.infrastructure.persistence.PmsTrackAudioFeatures;
import io.myforevermusic.api.modules.recommendation.infrastructure.local.InMemoryRecommendationAuditLogStore;
import io.myforevermusic.api.modules.recommendation.application.UserMusicEventService;
import io.myforevermusic.api.modules.recommendation.infrastructure.local.InMemoryUserMusicEventStore;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class GmsRecommendationFeedbackServiceTest {

    @Test
    void shouldRecordFeedbackForSyncedPmsLibraryTrack() {
        InMemoryPmsUserLibraryStore userLibraryStore = new InMemoryPmsUserLibraryStore();
        InMemoryUserMusicEventStore eventStore = new InMemoryUserMusicEventStore();
        InMemoryRecommendationAuditLogStore auditLogStore = new InMemoryRecommendationAuditLogStore();
        userLibraryStore.savePlaylists("user-001", List.of(samplePlaylist()));
        GmsRecommendationFeedbackService service = new GmsRecommendationFeedbackService(
            new InMemoryGmsRecommendationFeedbackStore(),
            userLibraryStore,
            new UserMusicEventService(eventStore),
            auditLogStore
        );

        GmsRecommendationFeedbackResponse response = service.recordFeedback(
            new GmsRecommendationFeedbackRequest(
                "user-001",
                "preview-001",
                "playlist-001",
                "track-001",
                "LIKE",
                1,
                "gms",
                "Strong match"
            )
        );

        assertThat(response.status()).isEqualTo("recorded");
        assertThat(response.feedback().feedbackId()).isEqualTo(1L);
        assertThat(response.feedback().feedbackType()).isEqualTo("like");
        assertThat(response.feedback().trackId()).isEqualTo("track-001");
        assertThat(eventStore.findRecentByUserId("user-001", 1).getFirst().eventType())
            .isEqualTo("recommendation_liked");
        assertThat(eventStore.findRecentByUserId("user-001", 1).getFirst().recommendationId())
            .isEqualTo("preview-001");
        assertThat(auditLogStore.findRecentByUserId("user-001", 1).getFirst().eventType())
            .isEqualTo("feedback_recorded");
        assertThat(auditLogStore.findRecentByUserId("user-001", 1).getFirst().feedbackType())
            .isEqualTo("like");
    }

    @Test
    void shouldRejectFeedbackForTrackOutsideUserLibrary() {
        GmsRecommendationFeedbackService service = new GmsRecommendationFeedbackService(
            new InMemoryGmsRecommendationFeedbackStore(),
            new InMemoryPmsUserLibraryStore(),
            new UserMusicEventService(new InMemoryUserMusicEventStore()),
            new InMemoryRecommendationAuditLogStore()
        );

        assertThatThrownBy(() -> service.recordFeedback(
            new GmsRecommendationFeedbackRequest(
                "user-001",
                "preview-001",
                "playlist-001",
                "track-missing",
                "like",
                1,
                "gms",
                null
            )
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("PMS library");
    }

    private PmsUserLibraryStore.LibraryPlaylistState samplePlaylist() {
        return new PmsUserLibraryStore.LibraryPlaylistState(
            "user-001",
            "playlist-001",
            "spotify-playlist-001",
            "Night Drive Archive",
            "spotify",
            "Forever Listener",
            "Synced from imported playlists.",
            null,
            "https://open.spotify.com/playlist/spotify-playlist-001",
            "spotify:playlist:spotify-playlist-001",
            Instant.parse("2026-05-04T00:00:00Z"),
            List.of(
                new PmsUserLibraryStore.LibraryTrackState(
                    "track-001",
                    "spotify-track-001",
                    "Midnight Receiver",
                    "Neon Bloom",
                    "spotify",
                    "synth-pop",
                    "Signal Bloom",
                    null,
                    "https://open.spotify.com/track/spotify-track-001",
                    "spotify:track:spotify-track-001",
                    null,
                    1,
                    true,
                    PmsTrackAudioFeatures.unresolved()
                )
            )
        );
    }
}
