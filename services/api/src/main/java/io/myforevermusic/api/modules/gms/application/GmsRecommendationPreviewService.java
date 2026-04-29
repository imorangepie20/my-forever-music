package io.myforevermusic.api.modules.gms.application;

import io.myforevermusic.api.modules.gms.infrastructure.ai.AiRecommendationPreviewClient;
import io.myforevermusic.api.modules.gms.presentation.GmsRecommendationPreviewRequest;
import io.myforevermusic.api.modules.gms.presentation.GmsRecommendationPreviewResponse;
import org.springframework.stereotype.Service;

@Service
public class GmsRecommendationPreviewService {

    private final AiRecommendationPreviewClient aiRecommendationPreviewClient;

    public GmsRecommendationPreviewService(AiRecommendationPreviewClient aiRecommendationPreviewClient) {
        this.aiRecommendationPreviewClient = aiRecommendationPreviewClient;
    }

    public GmsRecommendationPreviewResponse previewRecommendations(GmsRecommendationPreviewRequest request) {
        return aiRecommendationPreviewClient.requestPreview(request);
    }
}
