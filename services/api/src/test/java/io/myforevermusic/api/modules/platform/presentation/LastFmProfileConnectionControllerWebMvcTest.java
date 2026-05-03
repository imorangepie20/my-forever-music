package io.myforevermusic.api.modules.platform.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.myforevermusic.api.common.error.ApiExceptionHandler;
import io.myforevermusic.api.modules.platform.application.LastFmProfileConnectionService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LastFmProfileConnectionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class LastFmProfileConnectionControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LastFmProfileConnectionService lastFmProfileConnectionService;

    @Test
    void shouldSaveLastFmProfile() throws Exception {
        when(lastFmProfileConnectionService.connectProfile(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/platforms/lastfm/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(new LastFmProfileConnectRequest("user-001", "mibeen"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("connected"))
            .andExpect(jsonPath("$.connection.platform_id").value("last-fm"))
            .andExpect(jsonPath("$.connection.external_account_label").value("mibeen"));
    }

    private PlatformConnectionCommandResponse sampleResponse() {
        return new PlatformConnectionCommandResponse(
            "api",
            "connected",
            Instant.parse("2026-05-04T02:00:00Z"),
            new PlatformConnectionCommandResponse.ConnectionResult(
                "user-001",
                "last-fm",
                "Last.fm",
                true,
                "connected",
                "public-profile",
                "mibeen",
                "recent-scrobbles-read, top-artists-read, top-tracks-read",
                false,
                Instant.parse("2026-05-04T02:00:00Z")
            ),
            new PlatformConnectionCommandResponse.NextStep(
                "/platforms",
                "Last.fm signal profile saved. You can use it for EMS analysis or choose another PMS import source."
            )
        );
    }
}
