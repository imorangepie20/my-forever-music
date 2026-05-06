package io.myforevermusic.api.modules.platform.presentation;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.myforevermusic.api.modules.platform.application.PlatformCatalogService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PlatformCatalogController.class)
@AutoConfigureMockMvc(addFilters = false)
class PlatformCatalogControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlatformCatalogService platformCatalogService;

    @Test
    void shouldReturnPlatformCatalog() throws Exception {
        when(platformCatalogService.getCatalog()).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/platforms/catalog"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.service").value("api"))
            .andExpect(jsonPath("$.primary_audio_feature_source").value("provider-neutral-transition"))
            .andExpect(jsonPath("$.platforms[0].platform_id").value("spotify"))
            .andExpect(jsonPath("$.platforms[1].audio_feature_strategy").value("disabled-until-real-provider"));
    }

    private PlatformCatalogResponse sampleResponse() {
        return new PlatformCatalogResponse(
            "api",
            "ok",
            Instant.parse("2026-05-03T00:00:00Z"),
            "provider-neutral-transition",
            List.of("step-1", "step-2"),
            List.of(
                new PlatformCatalogResponse.PlatformOption(
                    "spotify",
                    "Spotify",
                    "priority-import-source",
                    true,
                    true,
                    "metadata-import-and-external-feature-backfill",
                    "PMS source",
                    "EMS source",
                    List.of("note-1")
                ),
                new PlatformCatalogResponse.PlatformOption(
                    "apple-music",
                    "Apple Music",
                    "planned-provider-not-enabled",
                    false,
                    true,
                    "disabled-until-real-provider",
                    "PMS provider not enabled",
                    "EMS source",
                    List.of("note-2")
                )
            )
        );
    }
}
