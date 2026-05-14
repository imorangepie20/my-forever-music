package io.myforevermusic.api.modules.recommendation.presentation;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.myforevermusic.api.modules.recommendation.application.RecommendationModelTrainingService;
import io.myforevermusic.api.modules.recommendation.application.SasrecModelRegistryAdminService;
import io.myforevermusic.api.modules.recommendation.infrastructure.ai.AiSasrecRegistryClient.SasrecRegistryResponse;
import io.myforevermusic.api.modules.recommendation.infrastructure.ai.AiSasrecTrainingClient;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SasrecModelRegistryAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class SasrecModelRegistryAdminControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SasrecModelRegistryAdminService adminService;

    @Test
    void shouldAutoTrainTargetUserWhenProvided() throws Exception {
        AiSasrecTrainingClient.SasrecTrainingOptions options =
            new AiSasrecTrainingClient.SasrecTrainingOptions(16, 7, 5, 16, 0.02d, true);
        when(adminService.autoTrainAndPromote(
            eq("admin-user"),
            eq("target-user"),
            eq(20),
            eq(10),
            eq(options)
        )).thenReturn(sampleAutoTrainResult());

        mockMvc.perform(post("/api/v1/recommendations/admin/sasrec/models/auto-train")
                .param("user_id", "admin-user")
                .param("target_user_id", "target-user")
                .param("event_limit", "20")
                .param("snapshot_limit", "10")
                .param("max_context_length", "16")
                .param("k", "7")
                .param("epochs", "5")
                .param("hidden_size", "16")
                .param("learning_rate", "0.02")
                .param("persist_artifact", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user_id").value("target-user"))
            .andExpect(jsonPath("$.training.user_id").value("target-user"))
            .andExpect(jsonPath("$.promote_result.user_id").value("target-user"))
            .andExpect(jsonPath("$.model_version").value("sasrec-mvp-target"));
    }

    private RecommendationModelTrainingService.AutoTrainResult sampleAutoTrainResult() {
        RecommendationModelTrainingResponse training = new RecommendationModelTrainingResponse(
            "sasrec-mvp-training",
            "ok",
            "target-user",
            new RecommendationModelTrainingResponse.DatasetSummary(
                20,
                10,
                3,
                2,
                5,
                "recommendation-sequence-v1",
                "sha256:target"
            ),
            "sasrec-mvp-target",
            Map.of("train_example_count", 4),
            Map.of("hit_rate_at_k", 0.5d),
            Map.of("hit_rate_at_k", 0.25d),
            Map.of("hit_rate_at_k", 0.25d),
            Map.of("qualified", true, "threshold", 0.0d, "reason", "target fixture qualified"),
            Map.of("saved", true),
            List.of()
        );
        SasrecRegistryResponse promoteResult = new SasrecRegistryResponse(
            "sasrec-model-registry",
            "ok",
            "target-user",
            "sasrec-mvp-target",
            "/tmp/sasrec/sasrec-mvp-target",
            "2026-05-14T00:00:00Z",
            5,
            4,
            "recommendation-sequence-v1",
            "sha256:target",
            List.of()
        );
        return new RecommendationModelTrainingService.AutoTrainResult(
            training,
            true,
            promoteResult,
            "qualified=true — sasrec-mvp-target 를 active model 로 promote 했습니다."
        );
    }
}
