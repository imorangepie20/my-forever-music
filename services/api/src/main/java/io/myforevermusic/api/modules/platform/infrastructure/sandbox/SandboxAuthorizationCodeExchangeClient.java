package io.myforevermusic.api.modules.platform.infrastructure.sandbox;

import io.myforevermusic.api.modules.platform.application.PlatformAuthorizationCodeExchangeClient;
import io.myforevermusic.api.modules.platform.application.PlatformAuthorizationSession;
import io.myforevermusic.api.modules.platform.application.PlatformTokenExchangeResult;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("sandbox")
public class SandboxAuthorizationCodeExchangeClient implements PlatformAuthorizationCodeExchangeClient {

    @Override
    public boolean supports(PlatformAuthorizationSession session) {
        return "sandbox-oauth".equals(session.authorizationMode());
    }

    @Override
    public PlatformTokenExchangeResult exchangeAuthorizationCode(
        PlatformAuthorizationSession session,
        String authorizationCode
    ) {
        Instant now = Instant.now();
        return new PlatformTokenExchangeResult(
            "sandbox-access-%s".formatted(UUID.randomUUID()),
            "sandbox-refresh-%s".formatted(UUID.randomUUID()),
            "Bearer",
            session.requestedScopes(),
            now.plus(1, ChronoUnit.HOURS)
        );
    }
}
