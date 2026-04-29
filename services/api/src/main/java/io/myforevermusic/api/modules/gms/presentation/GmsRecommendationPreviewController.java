package io.myforevermusic.api.modules.gms.presentation;

import io.myforevermusic.api.modules.gms.application.GmsRecommendationPreviewService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/gms/recommendations")
public class GmsRecommendationPreviewController {

    private final GmsRecommendationPreviewService gmsRecommendationPreviewService;

    public GmsRecommendationPreviewController(GmsRecommendationPreviewService gmsRecommendationPreviewService) {
        this.gmsRecommendationPreviewService = gmsRecommendationPreviewService;
    }

    @Operation(summary = "Request a preview recommendation set from the AI service")
    @PostMapping("/preview")
    public GmsRecommendationPreviewResponse previewRecommendations(@Valid @RequestBody GmsRecommendationPreviewRequest request) {
        return gmsRecommendationPreviewService.previewRecommendations(request);
    }
}
