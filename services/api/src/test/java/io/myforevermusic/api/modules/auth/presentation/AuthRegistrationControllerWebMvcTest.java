package io.myforevermusic.api.modules.auth.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.myforevermusic.api.common.error.ApiExceptionHandler;
import io.myforevermusic.api.modules.auth.application.AuthEmailAlreadyRegisteredException;
import io.myforevermusic.api.modules.auth.application.AuthRegistrationService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthRegistrationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class AuthRegistrationControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthRegistrationService authRegistrationService;

    @Test
    void shouldRegisterAccount() throws Exception {
        when(authRegistrationService.register(any())).thenReturn(sampleResponse());

        AuthRegistrationRequest request = new AuthRegistrationRequest(
            "Forever Listener",
            "listener@example.com",
            "music2026",
            "spotify",
            true,
            true,
            true
        );

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("registered"))
            .andExpect(jsonPath("$.user.email").value("listener@example.com"))
            .andExpect(jsonPath("$.onboarding.preferred_platform_id").value("spotify"))
            .andExpect(jsonPath("$.onboarding.next_step_path").value("/platforms"));
    }

    @Test
    void shouldRejectInvalidRequestBody() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "display_name": "",
                      "email": "not-an-email",
                      "password": "short",
                      "preferred_platform_id": "",
                      "accepted_terms": false,
                      "accepted_privacy_policy": false
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("error"))
            .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void shouldReturnConflictForDuplicateEmail() throws Exception {
        when(authRegistrationService.register(any()))
            .thenThrow(new AuthEmailAlreadyRegisteredException("listener@example.com"));

        AuthRegistrationRequest request = new AuthRegistrationRequest(
            "Forever Listener",
            "listener@example.com",
            "music2026",
            "spotify",
            false,
            true,
            true
        );

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("An account with this email already exists: listener@example.com"));
    }

    private AuthRegistrationResponse sampleResponse() {
        return new AuthRegistrationResponse(
            "api",
            "registered",
            Instant.parse("2026-05-03T03:00:00Z"),
            new AuthRegistrationResponse.RegisteredUser(
                "user-001",
                "listener@example.com",
                "Forever Listener",
                false
            ),
            new AuthRegistrationResponse.OnboardingState(
                "connect-platform",
                "spotify",
                true,
                "/platforms",
                "Connect your streaming service to start PMS import."
            )
        );
    }
}
