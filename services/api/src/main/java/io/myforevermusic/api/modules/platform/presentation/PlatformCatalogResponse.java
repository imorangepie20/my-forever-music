package io.myforevermusic.api.modules.platform.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PlatformCatalogResponse(
    String service,
    String status,
    Instant generatedAt,
    String primaryAudioFeatureSource,
    List<String> onboardingFlow,
    List<PlatformOption> platforms
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PlatformOption(
        String platformId,
        String displayName,
        String integrationStage,
        boolean pmsImportSupported,
        boolean emsCollectionSupported,
        String audioFeatureStrategy,
        String pmsRole,
        String emsRole,
        List<String> notes
    ) {
    }
}
