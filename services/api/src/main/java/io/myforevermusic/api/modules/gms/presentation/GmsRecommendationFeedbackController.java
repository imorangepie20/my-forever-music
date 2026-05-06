package io.myforevermusic.api.modules.gms.presentation;

import io.myforevermusic.api.modules.gms.application.GmsRecommendationFeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/gms/recommendations")
public class GmsRecommendationFeedbackController {

    private final GmsRecommendationFeedbackService feedbackService;

    public GmsRecommendationFeedbackController(GmsRecommendationFeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @Operation(summary = "Record user feedback for a GMS recommendation candidate")
    @PostMapping("/feedback")
    public GmsRecommendationFeedbackResponse recordFeedback(
        @Valid @RequestBody GmsRecommendationFeedbackRequest request
    ) {
        return feedbackService.recordFeedback(request);
    }
}
