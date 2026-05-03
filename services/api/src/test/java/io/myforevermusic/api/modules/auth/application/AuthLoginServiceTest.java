package io.myforevermusic.api.modules.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.myforevermusic.api.modules.auth.infrastructure.local.InMemoryAuthAccountStore;
import io.myforevermusic.api.modules.auth.presentation.AuthLoginRequest;
import io.myforevermusic.api.modules.auth.presentation.AuthRegistrationRequest;
import io.myforevermusic.api.modules.platform.application.PlatformConnectionService;
import io.myforevermusic.api.modules.platform.presentation.PlatformConnectionBootstrapResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AuthLoginServiceTest {

    @Test
    void shouldAuthenticateAccountAndReturnCurrentOnboardingState() {
        InMemoryAuthAccountStore authAccountStore = new InMemoryAuthAccountStore();
        AuthRegistrationService authRegistrationService = new AuthRegistrationService(
            authAccountStore,
            new BCryptPasswordEncoder()
        );
        String userId = authRegistrationService.register(new AuthRegistrationRequest(
            "Forever Listener",
            "listener@example.com",
            "music2026",
            "spotify",
            false,
            true,
            true
        )).user().userId();

        PlatformConnectionService platformConnectionService = mock(PlatformConnectionService.class);
        when(platformConnectionService.getBootstrap(userId)).thenReturn(sampleBootstrap(userId));

        AuthLoginService service = new AuthLoginService(
            authAccountStore,
            platformConnectionService,
            new BCryptPasswordEncoder()
        );

        var response = service.login(new AuthLoginRequest("listener@example.com", "music2026"));

        assertThat(response.status()).isEqualTo("authenticated");
        assertThat(response.user().userId()).isEqualTo(userId);
        assertThat(response.onboarding().nextStepPath()).isEqualTo("/pms");
        assertThat(response.onboarding().platformConnectionRequired()).isFalse();
    }

    @Test
    void shouldRejectWrongPassword() {
        InMemoryAuthAccountStore authAccountStore = new InMemoryAuthAccountStore();
        AuthRegistrationService authRegistrationService = new AuthRegistrationService(
            authAccountStore,
            new BCryptPasswordEncoder()
        );
        authRegistrationService.register(new AuthRegistrationRequest(
            "Forever Listener",
            "listener@example.com",
            "music2026",
            "spotify",
            false,
            true,
            true
        ));

        AuthLoginService service = new AuthLoginService(
            authAccountStore,
            mock(PlatformConnectionService.class),
            new BCryptPasswordEncoder()
        );

        assertThatThrownBy(() -> service.login(new AuthLoginRequest("listener@example.com", "wrongpass1")))
            .isInstanceOf(AuthInvalidCredentialsException.class)
            .hasMessage("Email or password is incorrect.");
    }

    @Test
    void shouldRejectUnknownEmail() {
        AuthLoginService service = new AuthLoginService(
            new InMemoryAuthAccountStore(),
            mock(PlatformConnectionService.class),
            new BCryptPasswordEncoder()
        );

        assertThatThrownBy(() -> service.login(new AuthLoginRequest("unknown@example.com", "music2026")))
            .isInstanceOf(AuthInvalidCredentialsException.class)
            .hasMessage("Email or password is incorrect.");
    }

    private PlatformConnectionBootstrapResponse sampleBootstrap(String userId) {
        return new PlatformConnectionBootstrapResponse(
            "api",
            "ok",
            Instant.parse("2026-05-04T02:00:00Z"),
            new PlatformConnectionBootstrapResponse.ConnectionUser(
                userId,
                "Forever Listener",
                "listener@example.com",
                "spotify",
                null,
                null
            ),
            new PlatformConnectionBootstrapResponse.ConnectionSummary(
                1,
                true,
                false,
                "import-playlists",
                "/pms",
                "Preferred platform is connected. You can continue into PMS import."
            ),
            List.of()
        );
    }
}
