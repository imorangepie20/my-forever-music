package io.myforevermusic.api.modules.recommendation.presentation;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.myforevermusic.api.modules.recommendation.application.RecommendationDatasetExportService;
import io.myforevermusic.api.modules.recommendation.application.RecommendationModelTrainingService;
import io.myforevermusic.api.modules.recommendation.infrastructure.ai.AiSasrecTrainingClient;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RecommendationDatasetExportController.class)
@AutoConfigureMockMvc(addFilters = false)
class RecommendationDatasetExportControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecommendationDatasetExportService datasetExportService;

    @MockBean
    private RecommendationModelTrainingService modelTrainingService;

    @Test
    void shouldExportUserSequenceDataset() throws Exception {
        when(datasetExportService.exportUserSequence(eq("user-001"), eq(20), eq(10))).thenReturn(
            new RecommendationDatasetExportResponse(
                "user-001",
                Instant.parse("2026-05-11T03:00:00Z"),
                20,
                10,
                new RecommendationDatasetExportResponse.Summary(1, 1, 2),
                List.of(),
                List.of(),
                List.of(new RecommendationDatasetExportResponse.SequenceItem(
                    "event",
                    1L,
                    "event:play_started:track-001",
                    "track-001",
                    "playlist-001",
                    null,
                    0.2,
                    Instant.parse("2026-05-11T01:00:00Z")
                ))
            )
        );

        mockMvc.perform(get("/api/v1/recommendations/datasets/users/user-001/sequence")
                .param("event_limit", "20")
                .param("snapshot_limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user_id").value("user-001"))
            .andExpect(jsonPath("$.summary.sequence_item_count").value(2))
            .andExpect(jsonPath("$.sequence[0].token").value("event:play_started:track-001"));
    }

    @Test
    void shouldTrainUserSasrecModelFromExportedDataset() throws Exception {
        when(modelTrainingService.trainSasrecModel(
            eq("user-001"),
            eq(20),
            eq(10),
            eq(new AiSasrecTrainingClient.SasrecTrainingOptions(4, 5, 3, 8, 0.02d, true))
        )).thenReturn(new RecommendationModelTrainingResponse(
            "sasrec-mvp-training",
            "ok",
            "user-001",
            new RecommendationModelTrainingResponse.DatasetSummary(20, 10, 3, 2, 5),
            "sasrec-mvp-test",
            Map.of("train_example_count", 4),
            Map.of("hit_rate_at_k", 0.5d),
            Map.of("hit_rate_at_k", 0.25d),
            Map.of("hit_rate_at_k", 0.25d),
            Map.of("saved", true),
            List.of()
        ));

        mockMvc.perform(post("/api/v1/recommendations/datasets/users/user-001/sasrec/train")
                .param("event_limit", "20")
                .param("snapshot_limit", "10")
                .param("max_context_length", "4")
                .param("k", "5")
                .param("epochs", "3")
                .param("hidden_size", "8")
                .param("learning_rate", "0.02")
                .param("persist_artifact", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.model_version").value("sasrec-mvp-test"))
            .andExpect(jsonPath("$.dataset_summary.sequence_item_count").value(5))
            .andExpect(jsonPath("$.model_artifact.saved").value(true));
    }
}
