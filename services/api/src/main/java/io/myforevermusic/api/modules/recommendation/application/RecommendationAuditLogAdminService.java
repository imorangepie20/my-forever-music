package io.myforevermusic.api.modules.recommendation.application;

import io.myforevermusic.api.modules.auth.application.AuthAccountStore;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RecommendationAuditLogAdminService {

    private static final String ADMIN_EMAIL = "jowoosungtidal@gmail.com";

    private final AuthAccountStore authAccountStore;
    private final RecommendationAuditLogStore auditLogStore;

    public RecommendationAuditLogAdminService(
        AuthAccountStore authAccountStore,
        RecommendationAuditLogStore auditLogStore
    ) {
        this.authAccountStore = authAccountStore;
        this.auditLogStore = auditLogStore;
    }

    public List<RecommendationAuditLogStore.StoredAuditLog> listRecent(
        String adminUserId,
        String targetUserId,
        int limit
    ) {
        assertAdmin(adminUserId);
        String resolvedTargetUserId = targetUserId == null || targetUserId.isBlank()
            ? adminUserId
            : targetUserId.trim();
        int safeLimit = Math.min(200, Math.max(1, limit));
        return auditLogStore.findRecentByUserId(resolvedTargetUserId, safeLimit);
    }

    private void assertAdmin(String userId) {
        String normalizedEmail = authAccountStore.findByUserId(userId)
            .map(account -> account.normalizedEmail())
            .orElse("");
        if (!ADMIN_EMAIL.equals(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Recommendation audit log admin access is restricted.");
        }
    }
}
