package io.myforevermusic.api.modules.recommendation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.myforevermusic.api.modules.recommendation.infrastructure.local.InMemoryUserMusicEventStore;
import io.myforevermusic.api.modules.recommendation.presentation.UserMusicEventRequest;
import io.myforevermusic.api.modules.recommendation.presentation.UserMusicEventResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class UserMusicEventServiceTest {

    @Test
    void shouldRecordPlaybackEventWithoutPmsLibraryDependency() {
        UserMusicEventService service = new UserMusicEventService(
            new InMemoryUserMusicEventStore(),
            new EventSignalWeights(),
            Clock.fixed(Instant.parse("2026-05-11T00:00:00Z"), ZoneOffset.UTC)
        );

        UserMusicEventResponse response = service.recordEvent(
            new UserMusicEventRequest(
                "user-001",
                "PLAY_COMPLETED",
                null,
                "spotify",
                "tidal",
                "ems-track-100",
                "track",
                "ems-track-100",
                "ems-playlist-001",
                "spotify-track-001",
                "spotify:track:spotify-track-001",
                "Midnight Receiver",
                "Neon Bloom",
                "Signal Bloom",
                "USRC17607839",
                180000,
                180000,
                1.0,
                null,
                0.8,
                Instant.parse("2026-05-10T23:59:00Z")
            )
        );

        assertThat(response.status()).isEqualTo("recorded");
        assertThat(response.event().eventId()).isEqualTo(1L);
        assertThat(response.event().eventType()).isEqualTo("play_completed");
        assertThat(response.event().eventWeight()).isEqualTo(1.0);
        assertThat(response.event().sourceSpace()).isEqualTo("player");
        assertThat(response.event().sourcePlatform()).isEqualTo("spotify");
        assertThat(response.event().playbackPlatformId()).isEqualTo("tidal");
        assertThat(response.event().playRatio()).isEqualTo(1.0);
    }

    @Test
    void shouldRejectUnsupportedEventType() {
        UserMusicEventService service = new UserMusicEventService(
            new InMemoryUserMusicEventStore(),
            new EventSignalWeights()
        );

        assertThatThrownBy(() -> service.recordEvent(
            new UserMusicEventRequest(
                "user-001",
                "unknown_event",
                "player",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            )
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("User music event type");
    }
}
