package io.myforevermusic.api.modules.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.myforevermusic.api.modules.auth.infrastructure.local.InMemoryAuthAccountStore;
import io.myforevermusic.api.modules.auth.presentation.AuthRegistrationRequest;
import io.myforevermusic.api.modules.auth.presentation.AuthRegistrationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AuthRegistrationServiceTest {

    @Test
    void shouldRegisterAccountAndReturnOnboardingStep() {
        AuthRegistrationService service = new AuthRegistrationService(
            new InMemoryAuthAccountStore(),
            new BCryptPasswordEncoder()
        );

        AuthRegistrationResponse response = service.register(new AuthRegistrationRequest(
            "Forever Listener",
            "listener@example.com",
            "music2026",
            "spotify",
            true,
            true,
            true
        ));

        assertThat(response.status()).isEqualTo("registered");
        assertThat(response.user().email()).isEqualTo("listener@example.com");
        assertThat(response.onboarding().preferredPlatformId()).isEqualTo("spotify");
        assertThat(response.onboarding().nextStepPath()).isEqualTo("/platforms");
    }

    @Test
    void shouldRejectDuplicateEmail() {
        AuthRegistrationService service = new AuthRegistrationService(
            new InMemoryAuthAccountStore(),
            new BCryptPasswordEncoder()
        );

        AuthRegistrationRequest request = new AuthRegistrationRequest(
            "Forever Listener",
            "listener@example.com",
            "music2026",
            "spotify",
            false,
            true,
            true
        );

        service.register(request);

        assertThatThrownBy(() -> service.register(request))
            .isInstanceOf(AuthEmailAlreadyRegisteredException.class)
            .hasMessageContaining("listener@example.com");
    }

    @Test
    void shouldRejectTidalUntilRealProviderIsImplemented() {
        AuthRegistrationService service = new AuthRegistrationService(
            new InMemoryAuthAccountStore(),
            new BCryptPasswordEncoder()
        );

        AuthRegistrationRequest request = new AuthRegistrationRequest(
            "Forever Listener",
            "tidal-listener@example.com",
            "music2026",
            "tidal",
            false,
            true,
            true
        );

        assertThatThrownBy(() -> service.register(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("real PMS playlist import");
    }

    @Test
    void shouldRejectLastFmAsPrimaryStreamingPlatform() {
        AuthRegistrationService service = new AuthRegistrationService(
            new InMemoryAuthAccountStore(),
            new BCryptPasswordEncoder()
        );

        AuthRegistrationRequest request = new AuthRegistrationRequest(
            "Forever Listener",
            "lastfm-primary@example.com",
            "music2026",
            "last-fm",
            false,
            true,
            true
        );

        assertThatThrownBy(() -> service.register(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("real PMS playlist import");
    }
}
