package io.myforevermusic.api.modules.ems.application;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.List;

public interface EmsAcquisitionSignalModel {
    EmsAcquisitionSignalModelResponse extractSignals(EmsAcquisitionSignalModelRequest request);

    record EmsAcquisitionSignalModelRequest(
        String sourceName,
        String sourceUrl,
        double sourceWeight,
        List<EmsEditorialArticle> articles,
        int maxSignals
    ) {
    }

    record EmsAcquisitionSignalModelResponse(
        String requestId,
        Instant generatedAt,
        String model,
        List<EmsAcquisitionSignal> signals
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record EmsAcquisitionSignal(
        String articleUrl,
        String articleTitle,
        String signalType,
        String query,
        double confidenceScore,
        String rationale
    ) {
    }
}
