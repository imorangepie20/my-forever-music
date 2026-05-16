package io.myforevermusic.api.modules.pms.presentation;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.myforevermusic.api.modules.pms.application.PmsTrackAudioFeaturesService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PmsTrackAudioFeaturesController.class)
@AutoConfigureMockMvc(addFilters = false)
class PmsTrackAudioFeaturesControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PmsTrackAudioFeaturesService audioFeaturesService;

    @Test
    void shouldReturnAudioFeaturesForFilledTrack() throws Exception {
        when(audioFeaturesService.getAudioFeatures("user-001", "spotify-track-001")).thenReturn(
            new PmsTrackAudioFeaturesResponse(
                "spotify-track-001",
                "reccobeats_lookup",
                true,
                215000,
                5,
                1,
                4,
                0.12,
                0.74,
                0.81,
                0.01,
                0.18,
                -5.4,
                0.05,
                124.0,
                0.62,
                Instant.parse("2026-05-10T12:00:00Z")
            )
        );

        mockMvc.perform(get("/api/v1/pms/tracks/spotify-track-001/audio-features")
                .queryParam("user_id", "user-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.audio_feature_track_id").value("spotify-track-001"))
            .andExpect(jsonPath("$.audio_features_filled").value(true))
            .andExpect(jsonPath("$.tempo").value(124.0))
            .andExpect(jsonPath("$.energy").value(0.81))
            .andExpect(jsonPath("$.valence").value(0.62));
    }

    @Test
    void shouldReturnUnresolvedWhenTrackHasNoFeatures() throws Exception {
        when(audioFeaturesService.getAudioFeatures("user-001", "unknown-track")).thenReturn(
            PmsTrackAudioFeaturesResponse.unresolved("unknown-track")
        );

        mockMvc.perform(get("/api/v1/pms/tracks/unknown-track/audio-features")
                .queryParam("user_id", "user-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.audio_feature_track_id").value("unknown-track"))
            .andExpect(jsonPath("$.audio_features_filled").value(false))
            .andExpect(jsonPath("$.audio_feature_source").value("unresolved"))
            .andExpect(jsonPath("$.tempo").doesNotExist());
    }
}
