package io.myforevermusic.api.modules.pms.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.myforevermusic.api.common.error.ApiExceptionHandler;
import io.myforevermusic.api.modules.pms.application.PmsPlaylistImportService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PmsPlaylistImportController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class PmsPlaylistImportControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PmsPlaylistImportService pmsPlaylistImportService;

    @Test
    void shouldReturnImportBootstrap() throws Exception {
        when(pmsPlaylistImportService.getBootstrap("user-001")).thenReturn(sampleBootstrapResponse());

        mockMvc.perform(get("/api/v1/pms/import/bootstrap").param("user_id", "user-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.summary.preferred_platform_connected").value(true))
            .andExpect(jsonPath("$.available_playlists[0].external_playlist_id").value("spotify-liked-night-drive"))
            .andExpect(jsonPath("$.imported_playlists[0].playlist_id").value("pms-spotify-spotify-liked-night-drive"));
    }

    @Test
    void shouldImportPlaylists() throws Exception {
        when(pmsPlaylistImportService.importPlaylists(any())).thenReturn(sampleImportResponse());

        PmsPlaylistImportRequest request = new PmsPlaylistImportRequest(
            "user-001",
            "spotify",
            List.of("spotify-liked-night-drive")
        );

        mockMvc.perform(post("/api/v1/pms/import/playlists")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("playlists_imported"))
            .andExpect(jsonPath("$.import_result.imported_playlist_count").value(1))
            .andExpect(jsonPath("$.next_step.path").value("/ems"));
    }

    private PmsPlaylistImportBootstrapResponse sampleBootstrapResponse() {
        return new PmsPlaylistImportBootstrapResponse(
            "api",
            "ok",
            Instant.parse("2026-05-03T09:00:00Z"),
            new PmsPlaylistImportBootstrapResponse.ImportUser(
                "user-001",
                "Forever Listener",
                "spotify"
            ),
            new PmsPlaylistImportBootstrapResponse.PreferredPlatformConnection(
                "spotify",
                "Spotify",
                true,
                "sandbox-oauth",
                "Forever Listener Spotify account",
                true
            ),
            new PmsPlaylistImportBootstrapResponse.ImportSummary(
                true,
                2,
                1,
                "/pms",
                "Choose connected platform playlists and import them into PMS."
            ),
            List.of(
                new PmsPlaylistImportBootstrapResponse.AvailablePlaylist(
                    "spotify-liked-night-drive",
                    "Liked Songs Night Drive",
                    "spotify",
                    3,
                    "spotify-library",
                    "High replay late-night mix from the connected Spotify account.",
                    false,
                    "complete_spotify_snapshot"
                )
            ),
            List.of(
                new PmsPlaylistImportBootstrapResponse.ImportedPlaylist(
                    "pms-spotify-spotify-liked-night-drive",
                    "spotify-liked-night-drive",
                    "Liked Songs Night Drive",
                    "spotify",
                    3,
                    Instant.parse("2026-05-03T09:05:00Z")
                )
            )
        );
    }

    private PmsPlaylistImportResponse sampleImportResponse() {
        return new PmsPlaylistImportResponse(
            "api",
            "playlists_imported",
            Instant.parse("2026-05-03T09:06:00Z"),
            new PmsPlaylistImportResponse.ImportResult(
                "user-001",
                "spotify",
                "Spotify",
                1,
                3,
                3,
                "sandbox-oauth"
            ),
            List.of(
                new PmsPlaylistImportResponse.ImportedPlaylistResult(
                    "pms-spotify-spotify-liked-night-drive",
                    "spotify-liked-night-drive",
                    "Liked Songs Night Drive",
                    "spotify",
                    3,
                    Instant.parse("2026-05-03T09:06:00Z")
                )
            ),
            new PmsPlaylistImportResponse.NextStep(
                "/ems",
                "Playlists were imported into PMS with complete Spotify audio feature snapshots. Continue to EMS analysis."
            )
        );
    }
}
