package io.myforevermusic.api.modules.auth.application;

import io.myforevermusic.api.modules.auth.presentation.AuthRegistrationRequest;
import io.myforevermusic.api.modules.auth.presentation.AuthRegistrationResponse;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthRegistrationService {

    private static final Set<String> SUPPORTED_PRIMARY_STREAMING_PLATFORM_IDS = Set.of(
        "spotify"
    );

    private final AuthAccountStore authAccountStore;
    private final PasswordEncoder passwordEncoder;

    public AuthRegistrationService(AuthAccountStore authAccountStore, PasswordEncoder passwordEncoder) {
        this.authAccountStore = authAccountStore;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthRegistrationResponse register(AuthRegistrationRequest request) {
        String email = normalizeEmail(request.email());
        String displayName = normalizeDisplayName(request.displayName());
        String preferredPlatformId = normalizePlatformId(request.preferredPlatformId());

        if (!SUPPORTED_PRIMARY_STREAMING_PLATFORM_IDS.contains(preferredPlatformId)) {
            throw new IllegalArgumentException(
                "Preferred platform must be a fully implemented streaming source that supports real PMS playlist import."
            );
        }

        Instant now = Instant.now();
        AuthRegisteredAccount account = authAccountStore.register(
            new AuthRegistrationDraft(
                "user-%s".formatted(UUID.randomUUID()),
                email,
                email.toLowerCase(Locale.ROOT),
                displayName,
                passwordEncoder.encode(request.password()),
                preferredPlatformId,
                request.marketingOptIn(),
                "connect-platform",
                now,
                now,
                now
            )
        );

        return new AuthRegistrationResponse(
            "api",
            "registered",
            account.registeredAt(),
            new AuthRegistrationResponse.RegisteredUser(
                account.userId(),
                account.email(),
                account.displayName(),
                false
            ),
            new AuthRegistrationResponse.OnboardingState(
                account.onboardingStage(),
                account.preferredPlatformId(),
                true,
                "/platforms",
                "Connect your streaming platform so PMS can preserve your playlists and taste library."
            )
        );
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim();
    }

    private String normalizeDisplayName(String displayName) {
        return displayName == null ? "" : displayName.trim();
    }

    private String normalizePlatformId(String preferredPlatformId) {
        return preferredPlatformId == null ? "" : preferredPlatformId.trim().toLowerCase(Locale.ROOT);
    }
}
