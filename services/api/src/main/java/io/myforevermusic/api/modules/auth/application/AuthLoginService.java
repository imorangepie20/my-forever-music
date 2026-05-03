package io.myforevermusic.api.modules.auth.application;

import io.myforevermusic.api.modules.auth.presentation.AuthLoginRequest;
import io.myforevermusic.api.modules.auth.presentation.AuthLoginResponse;
import io.myforevermusic.api.modules.auth.presentation.AuthRegistrationResponse;
import io.myforevermusic.api.modules.platform.application.PlatformConnectionService;
import io.myforevermusic.api.modules.platform.presentation.PlatformConnectionBootstrapResponse;
import java.time.Instant;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthLoginService {

    private final AuthAccountStore authAccountStore;
    private final PlatformConnectionService platformConnectionService;
    private final PasswordEncoder passwordEncoder;

    public AuthLoginService(
        AuthAccountStore authAccountStore,
        PlatformConnectionService platformConnectionService,
        PasswordEncoder passwordEncoder
    ) {
        this.authAccountStore = authAccountStore;
        this.platformConnectionService = platformConnectionService;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthLoginResponse login(AuthLoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        AuthAuthenticationAccount authenticationAccount = authAccountStore.findAuthenticationByNormalizedEmail(normalizedEmail)
            .orElseThrow(AuthInvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), authenticationAccount.passwordHash())) {
            throw new AuthInvalidCredentialsException();
        }

        AuthRegisteredAccount account = authenticationAccount.account();
        PlatformConnectionBootstrapResponse bootstrap = platformConnectionService.getBootstrap(account.userId());

        boolean platformConnectionRequired =
            bootstrap.summary().preferredPlatformReconnectRequired()
                || "connect-platform".equals(bootstrap.summary().onboardingStage());

        return new AuthLoginResponse(
            "api",
            "authenticated",
            Instant.now(),
            new AuthRegistrationResponse.RegisteredUser(
                account.userId(),
                account.email(),
                account.displayName(),
                false
            ),
            new AuthRegistrationResponse.OnboardingState(
                bootstrap.summary().onboardingStage(),
                account.preferredPlatformId(),
                platformConnectionRequired,
                bootstrap.summary().nextStepPath(),
                bootstrap.summary().nextStepMessage()
            )
        );
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
