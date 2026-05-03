package io.myforevermusic.api.modules.platform.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.myforevermusic.api.common.error.ApiExceptionHandler;
import io.myforevermusic.api.modules.platform.application.LastFmScrobbleSyncService;
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

@WebMvcTest(LastFmScrobbleSyncController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class LastFmScrobbleSyncControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LastFmScrobbleSyncService lastFmScrobbleSyncService;

    @Test
    void shouldReturnScrobbleBootstrap() throws Exception {
        when(lastFmScrobbleSyncService.getBootstrap("user-001")).thenReturn(sampleBootstrap());

        mockMvc.perform(get("/api/v1/platforms/lastfm/scrobbles/bootstrap")
                .queryParam("user_id", "user-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.last_fm_username").value("mibeen"))
            .andExpect(jsonPath("$.summary.stored_scrobble_count").value(2));
    }

    @Test
    void shouldSyncScrobbles() throws Exception {
        when(lastFmScrobbleSyncService.syncScrobbles(any())).thenReturn(sampleSyncResponse());

        mockMvc.perform(post("/api/v1/platforms/lastfm/scrobbles/sync")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(new LastFmScrobbleSyncRequest("user-001", 40))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("synced"))
            .andExpect(jsonPath("$.sync.inserted_scrobble_count").value(2))
            .andExpect(jsonPath("$.recent_scrobbles[0].artist_name").value("The Midnight"));
    }

    private LastFmScrobbleBootstrapResponse sampleBootstrap() {
        return new LastFmScrobbleBootstrapResponse(
            "api",
            "ok",
            Instant.parse("2026-05-04T04:00:00Z"),
            new LastFmScrobbleBootstrapResponse.BootstrapUser(
                "user-001",
                "mibeen",
                Instant.parse("2026-05-04T03:00:00Z")
            ),
            new LastFmScrobbleBootstrapResponse.BootstrapSummary(
                2,
                Instant.parse("2026-05-04T04:00:00Z"),
                2,
                "Recent Last.fm scrobbles are stored and ready for future EMS/GMS modeling."
            ),
            List.of(
                new LastFmScrobbleBootstrapResponse.ScrobbleItem(
                    "Days of Thunder",
                    "The Midnight",
                    "Days of Thunder",
                    "https://www.last.fm/music/The+Midnight/_/Days+of+Thunder",
                    null,
                    Instant.parse("2026-05-03T20:00:00Z"),
                    true,
                    Instant.parse("2026-05-04T04:00:00Z")
                )
            )
        );
    }

    private LastFmScrobbleSyncResponse sampleSyncResponse() {
        return new LastFmScrobbleSyncResponse(
            "api",
            "synced",
            Instant.parse("2026-05-04T04:00:00Z"),
            new LastFmScrobbleSyncResponse.SyncResult(
                "user-001",
                "mibeen",
                3,
                2,
                0,
                1,
                2,
                Instant.parse("2026-05-04T04:00:00Z")
            ),
            List.of(
                new LastFmScrobbleBootstrapResponse.ScrobbleItem(
                    "Days of Thunder",
                    "The Midnight",
                    "Days of Thunder",
                    "https://www.last.fm/music/The+Midnight/_/Days+of+Thunder",
                    null,
                    Instant.parse("2026-05-03T20:00:00Z"),
                    true,
                    Instant.parse("2026-05-04T04:00:00Z")
                )
            ),
            List.of("Stored scrobbles are deduplicated by username, played_at, artist, and track name.")
        );
    }
}
