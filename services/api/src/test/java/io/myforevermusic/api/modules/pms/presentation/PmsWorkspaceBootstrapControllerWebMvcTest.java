package io.myforevermusic.api.modules.pms.presentation;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.myforevermusic.api.modules.pms.application.PmsWorkspaceBootstrapService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PmsWorkspaceBootstrapController.class)
@AutoConfigureMockMvc(addFilters = false)
class PmsWorkspaceBootstrapControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PmsWorkspaceBootstrapService pmsWorkspaceBootstrapService;

    @Test
    void shouldReturnWorkspaceBootstrap() throws Exception {
        when(pmsWorkspaceBootstrapService.getWorkspaceBootstrap()).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/pms/workspace/bootstrap"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.service").value("api"))
            .andExpect(jsonPath("$.workspace_defaults.playlist_id").value("playlist-001"))
            .andExpect(jsonPath("$.playlists[0].title").value("Forever Midnight Drive"))
            .andExpect(jsonPath("$.suggested_tracks[0].track_id").value("track-alpha"))
            .andExpect(jsonPath("$.suggested_tracks[0].spotify_audio_features_filled").value(true))
            .andExpect(jsonPath("$.suggested_artists[1].artist_name").value("Artist Two"))
            .andExpect(jsonPath("$.suggested_genres[2].genre").value("indietronica"));
    }

    private PmsWorkspaceBootstrapResponse sampleResponse() {
        return new PmsWorkspaceBootstrapResponse(
            "api",
            "ok",
            Instant.parse("2026-04-30T01:00:00Z"),
            new PmsWorkspaceBootstrapResponse.WorkspaceDefaults(
                "user-001",
                "playlist-001",
                List.of("track-alpha", "track-beta"),
                List.of("Artist One"),
                List.of("synth-pop")
            ),
            List.of(
                new PmsWorkspaceBootstrapResponse.PlaylistOption(
                    "playlist-001",
                    "Forever Midnight Drive",
                    "spotify",
                    42,
                    "system",
                    "High replay consistency and strong synth-pop overlap."
                )
            ),
            List.of(
                new PmsWorkspaceBootstrapResponse.TrackSeedSuggestion(
                    "track-alpha",
                    "Track Alpha",
                    "Artist One",
                    "spotify",
                    "sp-track-alpha",
                    true,
                    "spotify_api"
                )
            ),
            List.of(
                new PmsWorkspaceBootstrapResponse.ArtistSeedSuggestion(
                    "Artist One",
                    0.94,
                    "Frequently co-occurs with the current seed tracks."
                ),
                new PmsWorkspaceBootstrapResponse.ArtistSeedSuggestion(
                    "Artist Two",
                    0.89,
                    "Strong affinity in the same replay cluster."
                )
            ),
            List.of(
                new PmsWorkspaceBootstrapResponse.GenreSeedSuggestion(
                    "synth-pop",
                    0.92,
                    "Core genre signal from the selected playlist."
                ),
                new PmsWorkspaceBootstrapResponse.GenreSeedSuggestion(
                    "dream-pop",
                    0.84,
                    "Supports softer mood transitions in EMS."
                ),
                new PmsWorkspaceBootstrapResponse.GenreSeedSuggestion(
                    "indietronica",
                    0.78,
                    "Good expansion edge for GMS preview diversity."
                )
            )
        );
    }
}
