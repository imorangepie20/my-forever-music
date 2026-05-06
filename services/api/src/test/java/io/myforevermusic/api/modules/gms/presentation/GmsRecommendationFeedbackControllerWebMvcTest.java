package io.myforevermusic.api.modules.gms.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.myforevermusic.api.modules.gms.application.GmsRecommendationFeedbackService;
import io.myforevermusic.api.modules.gms.application.GmsRecommendationFeedbackStore;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GmsRecommendationFeedbackController.class)
@AutoConfigureMockMvc(addFilters = false)
class GmsRecommendationFeedbackControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GmsRecommendationFeedbackService feedbackService;

    @Test
    void shouldRecordRecommendationFeedback() throws Exception {
        when(feedbackService.recordFeedback(any())).thenReturn(GmsRecommendationFeedbackResponse.from(
            new GmsRecommendationFeedbackStore.StoredFeedback(
                1L,
                "user-001",
                "preview-001",
                "playlist-001",
                "track-001",
                "like",
                1,
                "gms",
                "Strong match",
                Instant.parse("2026-05-05T00:00:00Z")
            )
        ));

        GmsRecommendationFeedbackRequest request = new GmsRecommendationFeedbackRequest(
            "user-001",
            "preview-001",
            "playlist-001",
            "track-001",
            "like",
            1,
            "gms",
            "Strong match"
        );

        mockMvc.perform(post("/api/v1/gms/recommendations/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("recorded"))
            .andExpect(jsonPath("$.feedback.feedback_id").value(1))
            .andExpect(jsonPath("$.feedback.feedback_type").value("like"));
    }

    @Test
    void shouldRejectMissingTrackId() throws Exception {
        mockMvc.perform(post("/api/v1/gms/recommendations/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "user_id": "user-001",
                      "feedback_type": "like"
                    }
                    """))
            .andExpect(status().isBadRequest());
    }
}
