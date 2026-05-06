package io.myforevermusic.api.modules.platform.infrastructure.persistence;

import io.myforevermusic.api.modules.platform.application.PlatformAccountCredential;
import io.myforevermusic.api.modules.platform.application.PlatformCredentialStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!local")
public class JpaPlatformCredentialStore implements PlatformCredentialStore {

    private final PlatformAccountCredentialRepository platformAccountCredentialRepository;

    public JpaPlatformCredentialStore(PlatformAccountCredentialRepository platformAccountCredentialRepository) {
        this.platformAccountCredentialRepository = platformAccountCredentialRepository;
    }

    @Override
    public java.util.Optional<PlatformAccountCredential> findByUserIdAndPlatformId(String userId, String platformId) {
        return platformAccountCredentialRepository.findByUserIdAndPlatformId(userId, platformId)
            .map(PlatformAccountCredentialEntity::toCredential);
    }

    @Override
    @Transactional
    public PlatformAccountCredential save(PlatformAccountCredential credential) {
        PlatformAccountCredentialEntity entity = platformAccountCredentialRepository
            .findByUserIdAndPlatformId(credential.userId(), credential.platformId())
            .orElseGet(() -> new PlatformAccountCredentialEntity(credential));

        entity.apply(credential);
        return platformAccountCredentialRepository.save(entity).toCredential();
    }

    @Override
    @Transactional
    public void clear(String userId, String platformId) {
        platformAccountCredentialRepository.deleteByUserIdAndPlatformId(userId, platformId);
    }
}
