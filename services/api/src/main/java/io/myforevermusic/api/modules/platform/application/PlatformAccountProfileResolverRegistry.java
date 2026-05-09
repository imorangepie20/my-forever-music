package io.myforevermusic.api.modules.platform.application;

import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PlatformAccountProfileResolverRegistry {

    private static final Logger log = LoggerFactory.getLogger(PlatformAccountProfileResolverRegistry.class);

    private final List<PlatformAccountProfileResolver> resolvers;

    public PlatformAccountProfileResolverRegistry(List<PlatformAccountProfileResolver> resolvers) {
        this.resolvers = resolvers;
    }

    public Optional<PlatformAccountProfile> resolve(PlatformAccountCredential credential) {
        return resolvers.stream()
            .filter(resolver -> resolver.supports(credential.platformId()))
            .findFirst()
            .flatMap(resolver -> resolveSafely(resolver, credential));
    }

    private Optional<PlatformAccountProfile> resolveSafely(
        PlatformAccountProfileResolver resolver,
        PlatformAccountCredential credential
    ) {
        try {
            return Optional.ofNullable(resolver.resolve(credential));
        } catch (RuntimeException exception) {
            log.warn(
                "Platform account profile resolution failed for user {} and platform {}: {}",
                credential.userId(),
                credential.platformId(),
                exception.getMessage()
            );
            return Optional.empty();
        }
    }
}
