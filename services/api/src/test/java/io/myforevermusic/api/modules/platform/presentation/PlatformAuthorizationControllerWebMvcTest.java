package io.myforevermusic.api.modules.platform.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.myforevermusic.api.common.error.ApiExceptionHandler;
import io.myforevermusic.api.modules.platform.application.PlatformAuthorizationService;
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

@WebMvcTest(PlatformAuthorizationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class PlatformAuthorizationControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PlatformAuthorizationService platformAuthorizationService;

    @Test
    void shouldStartAuthorization() throws Exception {
        when(platformAuthorizationService.startAuthorization(any())).thenReturn(sampleStartResponse());

        PlatformAuthorizationStartRequest request = new PlatformAuthorizationStartRequest("user-001", "spotify");

        mockMvc.perform(post("/api/v1/platforms/oauth/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("authorization_pending"))
            .andExpect(jsonPath("$.authorization.state").value("oauth-test-state"))
            .andExpect(jsonPath("$.authorization.approval_page_path").value("/platforms/oauth/authorize?state=oauth-test-state"));
    }

    @Test
    void shouldCompleteAuthorization() throws Exception {
        when(platformAuthorizationService.completeAuthorization(any())).thenReturn(sampleCompleteResponse());

        PlatformAuthorizationCompleteRequest request = new PlatformAuthorizationCompleteRequest(
            "user-001",
            "spotify",
            "oauth-test-state",
            "sandbox-approved",
            null
        );

        mockMvc.perform(post("/api/v1/platforms/oauth/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("authorization_completed"))
            .andExpect(jsonPath("$.connection.connected").value(true))
            .andExpect(jsonPath("$.next_step.path").value("/pms"));
    }

    private PlatformAuthorizationStartResponse sampleStartResponse() {
        return new PlatformAuthorizationStartResponse(
            "api",
            "authorization_pending",
            Instant.parse("2026-05-03T08:00:00Z"),
            new PlatformAuthorizationStartResponse.AuthorizationUser(
                "user-001",
                "Forever Listener",
                "listener@example.com"
            ),
            new PlatformAuthorizationStartResponse.AuthorizationSession(
                "oauth-test-state",
                "spotify",
                "Spotify",
                "sandbox-oauth",
                "internal_approval_page",
                List.of("playlist-read", "profile-read"),
                Instant.parse("2026-05-03T08:10:00Z"),
                "/platforms/oauth/authorize?state=oauth-test-state",
                "/platforms/oauth/callback?state=oauth-test-state&code=sandbox-approved",
                "sandbox-approved",
                null,
                null
            )
        );
    }

    private PlatformAuthorizationCompleteResponse sampleCompleteResponse() {
        return new PlatformAuthorizationCompleteResponse(
            "api",
            "authorization_completed",
            Instant.parse("2026-05-03T08:01:00Z"),
            new PlatformAuthorizationCompleteResponse.AuthorizationResult(
                "oauth-test-state",
                "spotify",
                "Spotify",
                "sandbox-oauth",
                List.of("playlist-read", "profile-read"),
                Instant.parse("2026-05-03T08:01:00Z")
            ),
            new PlatformAuthorizationCompleteResponse.ConnectionResult(
                "user-001",
                "spotify",
                true,
                "connected",
                "sandbox-oauth",
                "Forever Listener Spotify account",
                "playlist-read, profile-read",
                true,
                Instant.parse("2026-05-03T08:01:00Z")
            ),
            new PlatformAuthorizationCompleteResponse.NextStep(
                "/pms",
                "Preferred platform connected. Continue to PMS import."
            )
        );
    }
}
