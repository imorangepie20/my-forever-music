package io.myforevermusic.api.modules.recommendation.presentation;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.myforevermusic.api.modules.recommendation.application.RecommendationDatasetExportService;
import java.time.Instant;
import java.util.List;
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
}
