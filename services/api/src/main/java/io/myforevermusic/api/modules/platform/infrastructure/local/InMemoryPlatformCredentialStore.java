package io.myforevermusic.api.modules.platform.infrastructure.local;

import io.myforevermusic.api.modules.platform.application.PlatformAccountCredential;
import io.myforevermusic.api.modules.platform.application.PlatformCredentialStore;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class InMemoryPlatformCredentialStore implements PlatformCredentialStore {

    private final ConcurrentMap<String, PlatformAccountCredential> credentials = new ConcurrentHashMap<>();

    @Override
    public Optional<PlatformAccountCredential> findByUserIdAndPlatformId(String userId, String platformId) {
        return Optional.ofNullable(credentials.get(key(userId, platformId)));
    }

    @Override
    public PlatformAccountCredential save(PlatformAccountCredential credential) {
        credentials.put(key(credential.userId(), credential.platformId()), credential);
        return credential;
    }

    @Override
    public void clear(String userId, String platformId) {
        credentials.remove(key(userId, platformId));
    }

    private String key(String userId, String platformId) {
        return userId + "::" + platformId;
    }
}
