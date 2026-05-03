package io.myforevermusic.api.modules.platform.application;

import java.util.Optional;

public interface PlatformCredentialStore {

    Optional<PlatformAccountCredential> findByUserIdAndPlatformId(String userId, String platformId);

    PlatformAccountCredential save(PlatformAccountCredential credential);

    void clear(String userId, String platformId);
}
