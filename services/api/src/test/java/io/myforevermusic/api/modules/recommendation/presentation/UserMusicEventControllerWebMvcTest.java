package io.myforevermusic.api.modules.recommendation.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.myforevermusic.api.modules.recommendation.application.UserMusicEventService;
import io.myforevermusic.api.modules.recommendation.application.UserMusicEventStore;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserMusicEventController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserMusicEventControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserMusicEventService eventService;

    @Test
    void shouldRecordUserMusicEvent() throws Exception {
        when(eventService.recordEvent(any())).thenReturn(UserMusicEventResponse.from(
            new UserMusicEventStore.StoredEvent(
                1L,
                "user-001",
                "play_started",
                0.0,
                "player",
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
                0,
                0.0,
                null,
                0.8,
                Instant.parse("2026-05-11T00:00:00Z"),
                Instant.parse("2026-05-11T00:00:01Z")
            ),
            Instant.parse("2026-05-11T00:00:01Z")
        ));

        UserMusicEventRequest request = new UserMusicEventRequest(
            "user-001",
            "play_started",
            "player",
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
            0,
            0.0,
            null,
            0.8,
            Instant.parse("2026-05-11T00:00:00Z")
        );

        mockMvc.perform(post("/api/v1/recommendations/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("recorded"))
            .andExpect(jsonPath("$.event.event_id").value(1))
            .andExpect(jsonPath("$.event.event_type").value("play_started"))
            .andExpect(jsonPath("$.event.playback_platform_id").value("tidal"));
    }

    @Test
    void shouldRejectMissingUserId() throws Exception {
        mockMvc.perform(post("/api/v1/recommendations/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "event_type": "play_started"
                    }
                    """))
            .andExpect(status().isBadRequest());
    }
}
