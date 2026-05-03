package io.myforevermusic.api.modules.platform.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.myforevermusic.api.common.error.ApiExceptionHandler;
import io.myforevermusic.api.modules.platform.application.PlatformConnectionService;
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

@WebMvcTest(PlatformConnectionBootstrapController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class PlatformConnectionBootstrapControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PlatformConnectionService platformConnectionService;

    @Test
    void shouldReturnConnectionBootstrap() throws Exception {
        when(platformConnectionService.getBootstrap("user-001")).thenReturn(sampleBootstrap());

        mockMvc.perform(get("/api/v1/platforms/connections/bootstrap").param("user_id", "user-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.user_id").value("user-001"))
            .andExpect(jsonPath("$.summary.preferred_platform_connected").value(false))
            .andExpect(jsonPath("$.connections[0].platform_id").value("spotify"));
    }

    @Test
    void shouldConnectPlatform() throws Exception {
        when(platformConnectionService.connect(any())).thenReturn(sampleCommand("connected", true));

        PlatformConnectRequest request = new PlatformConnectRequest("user-001", "spotify", "sandbox", "Forever Listener Spotify");

        mockMvc.perform(post("/api/v1/platforms/connections/connect")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("connected"))
            .andExpect(jsonPath("$.connection.connected").value(true))
            .andExpect(jsonPath("$.next_step.path").value("/pms"));
    }

    @Test
    void shouldDisconnectPlatform() throws Exception {
        when(platformConnectionService.disconnect(any())).thenReturn(sampleCommand("disconnected", false));

        PlatformDisconnectRequest request = new PlatformDisconnectRequest("user-001", "spotify");

        mockMvc.perform(post("/api/v1/platforms/connections/disconnect")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("disconnected"))
            .andExpect(jsonPath("$.connection.connected").value(false));
    }

    private PlatformConnectionBootstrapResponse sampleBootstrap() {
        return new PlatformConnectionBootstrapResponse(
            "api",
            "ok",
            Instant.parse("2026-05-03T07:00:00Z"),
            new PlatformConnectionBootstrapResponse.ConnectionUser(
                "user-001",
                "Forever Listener",
                "listener@example.com",
                "spotify"
            ),
            new PlatformConnectionBootstrapResponse.ConnectionSummary(
                0,
                false,
                "connect-platform",
                "/platforms",
                "Connect your preferred platform first, then continue to PMS import."
            ),
            List.of(
                new PlatformConnectionBootstrapResponse.PlatformConnectionCard(
                    "spotify",
                    "Spotify",
                    true,
                    false,
                    "not_connected",
                    null,
                    null,
                    false,
                    null,
                    "Connect"
                )
            )
        );
    }

    private PlatformConnectionCommandResponse sampleCommand(String status, boolean connected) {
        return new PlatformConnectionCommandResponse(
            "api",
            status,
            Instant.parse("2026-05-03T07:00:00Z"),
            new PlatformConnectionCommandResponse.ConnectionResult(
                "user-001",
                "spotify",
                "Spotify",
                connected,
                connected ? "connected" : "not_connected",
                "sandbox",
                "Forever Listener Spotify",
                "playlist-read, profile-read",
                connected,
                connected ? Instant.parse("2026-05-03T07:00:00Z") : null
            ),
            new PlatformConnectionCommandResponse.NextStep(
                connected ? "/pms" : "/platforms",
                connected ? "Preferred platform connected. Continue to PMS import."
                    : "You can connect more platforms or continue the onboarding setup."
            )
        );
    }
}
