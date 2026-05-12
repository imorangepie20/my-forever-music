package io.myforevermusic.api.modules.recommendation.application;

import io.myforevermusic.api.modules.auth.application.AuthAccountStore;
import io.myforevermusic.api.modules.recommendation.infrastructure.ai.AiSasrecRegistryClient;
import io.myforevermusic.api.modules.recommendation.infrastructure.ai.AiSasrecRegistryClient.SasrecRegistryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SasrecModelRegistryAdminService {

    private static final String ADMIN_EMAIL = "jowoosungtidal@gmail.com";

    private final AiSasrecRegistryClient registryClient;
    private final AuthAccountStore authAccountStore;

    public SasrecModelRegistryAdminService(
        AiSasrecRegistryClient registryClient,
        AuthAccountStore authAccountStore
    ) {
        this.registryClient = registryClient;
        this.authAccountStore = authAccountStore;
    }

    public SasrecRegistryResponse latest(String adminUserId) {
        assertAdmin(adminUserId);
        return registryClient.latest(adminUserId);
    }

    public SasrecRegistryResponse promote(String adminUserId, String modelVersion) {
        assertAdmin(adminUserId);
        if (modelVersion == null || modelVersion.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "model_version is required.");
        }
        return registryClient.promote(adminUserId, modelVersion);
    }

    public SasrecRegistryResponse disable(String adminUserId, String modelVersion) {
        assertAdmin(adminUserId);
        if (modelVersion == null || modelVersion.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "model_version is required.");
        }
        return registryClient.disable(adminUserId, modelVersion);
    }

    public SasrecRegistryResponse rollback(String adminUserId) {
        assertAdmin(adminUserId);
        return registryClient.rollback(adminUserId);
    }

    private void assertAdmin(String userId) {
        String normalizedEmail = authAccountStore.findByUserId(userId)
            .map(account -> account.normalizedEmail())
            .orElse("");
        if (!ADMIN_EMAIL.equals(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "SASRec model registry admin access is restricted.");
        }
    }
}
