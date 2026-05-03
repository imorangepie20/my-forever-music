package io.myforevermusic.api.modules.platform.application;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PlatformPlaylistProviderRegistry {

    private final List<PlatformPlaylistProvider> providers;

    public PlatformPlaylistProviderRegistry(List<PlatformPlaylistProvider> providers) {
        this.providers = List.copyOf(providers);
    }

    public PlatformPlaylistProvider getRequiredProvider(String platformId, PlatformAccountCredential credential) {
        return providers.stream()
            .filter(provider -> provider.supports(platformId, credential))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "No platform playlist provider is available for platform: %s".formatted(platformId)
            ));
    }
}
