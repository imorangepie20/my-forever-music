package io.myforevermusic.api.modules.ems.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.myforevermusic.api.modules.ems.presentation.EmsWorkspaceAnalysisRequest;
import io.myforevermusic.api.modules.ems.presentation.EmsWorkspaceAnalysisResponse;
import io.myforevermusic.api.modules.pms.infrastructure.persistence.PmsCatalogTrackEntity;
import io.myforevermusic.api.modules.pms.infrastructure.persistence.PmsCatalogTrackRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EmsWorkspaceAnalysisServiceTest {

    @Test
    void shouldRecommendCalmerProfileFromTextSeedsOnly() {
        EmsWorkspaceAnalysisService service = new EmsWorkspaceAnalysisService(Optional.empty(), Optional.empty());

        EmsWorkspaceAnalysisResponse response = service.analyzeWorkspace(
            new EmsWorkspaceAnalysisRequest(
                "user-001",
                "playlist-002",
                List.of(),
                List.of("Artist Four"),
                List.of("ambient-pop", "dream-pop")
            )
        );

        assertThat(response.workspaceRecommendation().mood()).isEqualTo("calm");
        assertThat(response.workspaceRecommendation().energyLevel()).isLessThanOrEqualTo(3);
        assertThat(response.topSignals()).isNotEmpty();
    }

    @Test
    void shouldUseCatalogTracksWhenAvailable() {
        PmsCatalogTrackRepository trackRepository = mock(PmsCatalogTrackRepository.class);
        when(trackRepository.findAllById(anyIterable())).thenReturn(List.of(
            new PmsCatalogTrackEntity("track-alpha", "Track Alpha", "Artist One", "spotify", "synth-pop"),
            new PmsCatalogTrackEntity("track-gamma", "Track Gamma", "Artist One", "spotify", "indietronica")
        ));

        EmsWorkspaceAnalysisService service = new EmsWorkspaceAnalysisService(
            Optional.of(trackRepository),
            Optional.empty()
        );

        EmsWorkspaceAnalysisResponse response = service.analyzeWorkspace(
            new EmsWorkspaceAnalysisRequest(
                "user-001",
                "playlist-001",
                List.of("track-alpha", "track-gamma"),
                List.of(),
                List.of("synth-pop")
            )
        );

        assertThat(response.context().matchedCatalogTrackCount()).isEqualTo(2);
        assertThat(response.workspaceRecommendation().familiarityBias()).isGreaterThanOrEqualTo(4);
        assertThat(response.notes()).anyMatch(note -> note.contains("Matched catalog tracks"));
    }
}
