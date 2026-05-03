package io.myforevermusic.api.modules.platform.application;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PlatformAuthorizationCodeExchangeRegistry {

    private final List<PlatformAuthorizationCodeExchangeClient> clients;

    public PlatformAuthorizationCodeExchangeRegistry(List<PlatformAuthorizationCodeExchangeClient> clients) {
        this.clients = List.copyOf(clients);
    }

    public PlatformAuthorizationCodeExchangeClient getRequiredClient(PlatformAuthorizationSession session) {
        return clients.stream()
            .filter(client -> client.supports(session))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "No authorization code exchange client is available for mode: %s".formatted(session.authorizationMode())
            ));
    }
}
