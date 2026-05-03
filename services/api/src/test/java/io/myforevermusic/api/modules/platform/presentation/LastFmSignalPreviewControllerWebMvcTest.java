package io.myforevermusic.api.modules.platform.presentation;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.myforevermusic.api.modules.platform.application.LastFmSignalPreviewService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LastFmSignalPreviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class LastFmSignalPreviewControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LastFmSignalPreviewService lastFmSignalPreviewService;

    @Test
    void shouldReturnLastFmSignalPreview() throws Exception {
        when(lastFmSignalPreviewService.getPreview("mibeen", "1month", 8, 6))
            .thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/platforms/lastfm/preview")
                .queryParam("username", "mibeen"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.service").value("api"))
            .andExpect(jsonPath("$.request.username").value("mibeen"))
            .andExpect(jsonPath("$.summary.source").value("lastfm-public-api"))
            .andExpect(jsonPath("$.top_artists[0].artist_name").value("The Midnight"));
    }

    private LastFmSignalPreviewResponse sampleResponse() {
        return new LastFmSignalPreviewResponse(
            "api",
            "ok",
            Instant.parse("2026-05-04T00:00:00Z"),
            new LastFmSignalPreviewResponse.PreviewRequest("mibeen", "1month", 8, 6),
            new LastFmSignalPreviewResponse.LastFmUserProfile(
                "mibeen",
                "Woo Sung Jo",
                "KR",
                54189L,
                "https://www.last.fm/user/mibeen",
                null,
                Instant.parse("2020-01-01T00:00:00Z")
            ),
            new LastFmSignalPreviewResponse.PreviewSummary(
                "lastfm-public-api",
                2,
                2,
                2,
                true,
                2,
                "Use top artists as EMS affinity seeds or keep Last.fm as a long-term taste signal source."
            ),
            List.of(
                new LastFmSignalPreviewResponse.SignalInsight(
                    "artist-anchor",
                    "Long-Term Artist Anchor",
                    "The Midnight is the strongest 1month artist signal right now with 88 plays."
                )
            ),
            List.of(
                new LastFmSignalPreviewResponse.RecentTrack(
                    "Days of Thunder",
                    "The Midnight",
                    "Days of Thunder",
                    "https://www.last.fm/music/The+Midnight/_/Days+of+Thunder",
                    null,
                    true,
                    null,
                    true
                )
            ),
            List.of(
                new LastFmSignalPreviewResponse.TopArtist(
                    "The Midnight",
                    1,
                    88L,
                    "https://www.last.fm/music/The+Midnight",
                    null
                )
            ),
            List.of(
                new LastFmSignalPreviewResponse.TopTrack(
                    "Days of Thunder",
                    "The Midnight",
                    1,
                    24L,
                    "https://www.last.fm/music/The+Midnight/_/Days+of+Thunder",
                    "https://www.last.fm/music/The+Midnight",
                    null
                )
            )
        );
    }
}
