package io.myforevermusic.api.modules.platform.application;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PlatformTokenRefreshRegistry {

    private final List<PlatformTokenRefreshClient> clients;

    public PlatformTokenRefreshRegistry(List<PlatformTokenRefreshClient> clients) {
        this.clients = List.copyOf(clients);
    }

    public boolean supports(PlatformAccountCredential credential) {
        return clients.stream().anyMatch(client -> client.supports(credential));
    }

    public PlatformTokenRefreshClient getRequiredClient(PlatformAccountCredential credential) {
        return clients.stream()
            .filter(client -> client.supports(credential))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "No platform token refresh client is available for platform: %s".formatted(credential.platformId())
            ));
    }
}
