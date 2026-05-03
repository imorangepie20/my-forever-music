package io.myforevermusic.api.modules.auth.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.myforevermusic.api.common.error.ApiExceptionHandler;
import io.myforevermusic.api.modules.auth.application.AuthInvalidCredentialsException;
import io.myforevermusic.api.modules.auth.application.AuthLoginService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthLoginController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class AuthLoginControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthLoginService authLoginService;

    @Test
    void shouldAuthenticateUser() throws Exception {
        when(authLoginService.login(any())).thenReturn(sampleResponse());

        AuthLoginRequest request = new AuthLoginRequest(
            "listener@example.com",
            "music2026"
        );

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("authenticated"))
            .andExpect(jsonPath("$.user.email").value("listener@example.com"))
            .andExpect(jsonPath("$.onboarding.next_step_path").value("/pms"));
    }

    @Test
    void shouldRejectInvalidCredentials() throws Exception {
        when(authLoginService.login(any())).thenThrow(new AuthInvalidCredentialsException());

        AuthLoginRequest request = new AuthLoginRequest(
            "listener@example.com",
            "wrongpass1"
        );

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("invalid_credentials"))
            .andExpect(jsonPath("$.message").value("Email or password is incorrect."));
    }

    @Test
    void shouldRejectInvalidRequestBody() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "not-an-email",
                      "password": ""
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("error"))
            .andExpect(jsonPath("$.errors").isArray());
    }

    private AuthLoginResponse sampleResponse() {
        return new AuthLoginResponse(
            "api",
            "authenticated",
            Instant.parse("2026-05-04T02:30:00Z"),
            new AuthRegistrationResponse.RegisteredUser(
                "user-001",
                "listener@example.com",
                "Forever Listener",
                false
            ),
            new AuthRegistrationResponse.OnboardingState(
                "import-playlists",
                "spotify",
                false,
                "/pms",
                "Preferred platform is connected. You can continue into PMS import."
            )
        );
    }
}
