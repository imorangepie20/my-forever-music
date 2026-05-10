package io.myforevermusic.api.modules.ems.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.myforevermusic.api.modules.ems.application.EmsOverviewService;
import io.myforevermusic.api.modules.ems.application.EmsWorkspaceAnalysisService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EmsWorkspaceAnalysisController.class)
@AutoConfigureMockMvc(addFilters = false)
class EmsWorkspaceAnalysisControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmsWorkspaceAnalysisService emsWorkspaceAnalysisService;

    @MockBean
    private EmsOverviewService emsOverviewService;

    @Test
    void shouldReturnWorkspaceAnalysis() throws Exception {
        when(emsWorkspaceAnalysisService.analyzeWorkspace(any())).thenReturn(sampleResponse());

        EmsWorkspaceAnalysisRequest request = new EmsWorkspaceAnalysisRequest(
            "user-001",
            "playlist-001",
            List.of("track-alpha"),
            List.of("Artist One"),
            List.of("synth-pop")
        );

        mockMvc.perform(post("/api/v1/ems/workspace/analysis")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.service").value("api"))
            .andExpect(jsonPath("$.context.strategy").value("catalog-signal-analysis-v1"))
            .andExpect(jsonPath("$.workspace_recommendation.mood").value("upbeat"))
            .andExpect(jsonPath("$.top_signals[0].label").value("synth-pop"));
    }

    private EmsWorkspaceAnalysisResponse sampleResponse() {
        return new EmsWorkspaceAnalysisResponse(
            "api",
            "ok",
            Instant.parse("2026-05-02T09:00:00Z"),
            new EmsWorkspaceAnalysisResponse.AnalysisContext(
                "catalog-signal-analysis-v1",
                "playlist-001",
                2,
                1,
                1,
                1
            ),
            new EmsWorkspaceAnalysisResponse.WorkspaceRecommendation(
                "upbeat",
                4,
                4,
                0.86
            ),
            List.of(
                new EmsWorkspaceAnalysisResponse.SignalCard(
                    "genre",
                    "synth-pop",
                    1.8,
                    "Synth-pop pushes the session toward repeatable uplift and bright momentum."
                )
            ),
            List.of("Synth-pop carries the strongest lift, so EMS is biasing toward upbeat motion."),
            List.of()
        );
    }
}
