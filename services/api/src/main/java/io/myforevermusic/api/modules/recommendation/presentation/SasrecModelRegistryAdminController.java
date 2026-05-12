package io.myforevermusic.api.modules.recommendation.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.myforevermusic.api.modules.recommendation.application.SasrecModelRegistryAdminService;
import io.myforevermusic.api.modules.recommendation.infrastructure.ai.AiSasrecRegistryClient.SasrecRegistryResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recommendations/admin/sasrec/models")
public class SasrecModelRegistryAdminController {

    private final SasrecModelRegistryAdminService adminService;

    public SasrecModelRegistryAdminController(SasrecModelRegistryAdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(summary = "Resolve the active SASRec model for the configured admin user")
    @GetMapping("/latest")
    public SasrecRegistryAdminResponse getLatest(@RequestParam("user_id") String userId) {
        return SasrecRegistryAdminResponse.from(adminService.latest(userId));
    }

    @Operation(summary = "Promote a SASRec model version as active for the configured admin user")
    @PostMapping("/{modelVersion}/promote")
    public SasrecRegistryAdminResponse promote(
        @PathVariable String modelVersion,
        @RequestParam("user_id") String userId
    ) {
        return SasrecRegistryAdminResponse.from(adminService.promote(userId, modelVersion));
    }

    @Operation(summary = "Disable a SASRec model version from being served")
    @PostMapping("/{modelVersion}/disable")
    public SasrecRegistryAdminResponse disable(
        @PathVariable String modelVersion,
        @RequestParam("user_id") String userId
    ) {
        return SasrecRegistryAdminResponse.from(adminService.disable(userId, modelVersion));
    }

    @Operation(summary = "Roll back the active SASRec model to the previously promoted version")
    @PostMapping("/rollback")
    public SasrecRegistryAdminResponse rollback(@RequestParam("user_id") String userId) {
        return SasrecRegistryAdminResponse.from(adminService.rollback(userId));
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SasrecRegistryAdminResponse(
        String service,
        String status,
        Instant generatedAt,
        String userId,
        String modelVersion,
        String artifactDir,
        String generatedAtAi,
        Integer vocabularySize,
        Integer trainExampleCount,
        java.util.List<String> warnings
    ) {
        static SasrecRegistryAdminResponse from(SasrecRegistryResponse response) {
            return new SasrecRegistryAdminResponse(
                "api",
                response.status(),
                Instant.now(),
                response.userId(),
                response.modelVersion(),
                response.artifactDir(),
                response.generatedAt(),
                response.vocabularySize(),
                response.trainExampleCount(),
                response.warnings() == null ? java.util.List.of() : response.warnings()
            );
        }
    }
}
