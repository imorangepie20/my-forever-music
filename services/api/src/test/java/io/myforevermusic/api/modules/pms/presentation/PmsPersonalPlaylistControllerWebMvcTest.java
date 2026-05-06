package io.myforevermusic.api.modules.pms.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.myforevermusic.api.modules.pms.application.PmsPersonalPlaylistService;
import io.myforevermusic.api.modules.pms.application.PmsPersonalPlaylistStore;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PmsPersonalPlaylistController.class)
@AutoConfigureMockMvc(addFilters = false)
class PmsPersonalPlaylistControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PmsPersonalPlaylistService personalPlaylistService;

    @Test
    void shouldReturnPersonalPlaylistBootstrap() throws Exception {
        when(personalPlaylistService.bootstrap("user-001")).thenReturn(
            PmsPersonalPlaylistBootstrapResponse.from("user-001", List.of(samplePlaylist()))
        );

        mockMvc.perform(get("/api/v1/pms/personal-playlists/bootstrap")
                .queryParam("user_id", "user-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.summary.playlist_count").value(1))
            .andExpect(jsonPath("$.playlists[0].playlist_id").value("personal-saved-gms-recommendations"))
            .andExpect(jsonPath("$.playlists[0].tracks[0].audio_feature_track_id").value("spotify-track-001"));
    }

    @Test
    void shouldCreatePersonalPlaylist() throws Exception {
        when(personalPlaylistService.createPlaylist(any())).thenReturn(
            PmsPersonalPlaylistCommandResponse.from("created", samplePlaylist(), "Created.")
        );

        PmsPersonalPlaylistCreateRequest request = new PmsPersonalPlaylistCreateRequest(
            "user-001",
            "Late Night Saves",
            "Tracks worth keeping."
        );

        mockMvc.perform(post("/api/v1/pms/personal-playlists")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("created"))
            .andExpect(jsonPath("$.playlist.track_count").value(1));
    }

    @Test
    void shouldSaveTrackIntoPersonalPlaylist() throws Exception {
        when(personalPlaylistService.saveTrack(any())).thenReturn(
            PmsPersonalPlaylistCommandResponse.from("saved", samplePlaylist(), "Saved.")
        );

        PmsPersonalPlaylistTrackSaveRequest request = new PmsPersonalPlaylistTrackSaveRequest(
            "user-001",
            null,
            "Saved GMS Recommendations",
            "track-001",
            "gms-preview"
        );

        mockMvc.perform(post("/api/v1/pms/personal-playlists/tracks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("saved"))
            .andExpect(jsonPath("$.playlist.tracks[0].track_id").value("track-001"))
            .andExpect(jsonPath("$.playlist.tracks[0].audio_feature_track_id").value("spotify-track-001"));
    }

    @Test
    void shouldRejectMissingTrackId() throws Exception {
        mockMvc.perform(post("/api/v1/pms/personal-playlists/tracks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "user_id": "user-001"
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    private PmsPersonalPlaylistStore.PersonalPlaylistState samplePlaylist() {
        return new PmsPersonalPlaylistStore.PersonalPlaylistState(
            "user-001",
            "personal-saved-gms-recommendations",
            "Saved GMS Recommendations",
            "Tracks saved from GMS recommendation candidates.",
            Instant.parse("2026-05-05T00:00:00Z"),
            Instant.parse("2026-05-05T00:01:00Z"),
            List.of(
                new PmsPersonalPlaylistStore.PersonalTrackState(
                    "track-001",
                    "Midnight Receiver",
                    "Neon Bloom",
                    "spotify",
                    "Signal Bloom",
                    null,
                    "https://open.spotify.com/track/spotify-track-001",
                    "spotify:track:spotify-track-001",
                    null,
                    "spotify-track-001",
                    218000,
                    1,
                    "gms-preview",
                    Instant.parse("2026-05-05T00:01:00Z")
                )
            )
        );
    }
}
