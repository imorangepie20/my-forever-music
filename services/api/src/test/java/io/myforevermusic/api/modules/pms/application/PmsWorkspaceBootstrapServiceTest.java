package io.myforevermusic.api.modules.pms.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.myforevermusic.api.modules.pms.presentation.PmsWorkspaceBootstrapResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PmsWorkspaceBootstrapServiceTest {

    @Test
    void shouldUseFirstNonEmptySource() {
        PmsWorkspaceBootstrapResponse expected = sampleResponse("playlist-db");
        PmsWorkspaceBootstrapService service = new PmsWorkspaceBootstrapService(
            List.of(
                (userId, playlistId) -> Optional.empty(),
                (userId, playlistId) -> Optional.of(expected),
                (userId, playlistId) -> Optional.of(sampleResponse("playlist-fallback"))
            )
        );

        PmsWorkspaceBootstrapResponse actual = service.getWorkspaceBootstrap(null, null);

        assertThat(actual.workspaceDefaults().playlistId()).isEqualTo("playlist-db");
    }

    @Test
    void shouldFailWhenNoSourceReturnsData() {
        PmsWorkspaceBootstrapService service = new PmsWorkspaceBootstrapService(
            List.of((userId, playlistId) -> Optional.empty())
        );

        assertThatThrownBy(() -> service.getWorkspaceBootstrap(null, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No PMS workspace bootstrap source");
    }

    private PmsWorkspaceBootstrapResponse sampleResponse(String playlistId) {
        return new PmsWorkspaceBootstrapResponse(
            "api",
            "ok",
            Instant.parse("2026-04-30T00:00:00Z"),
            new PmsWorkspaceBootstrapResponse.WorkspaceDefaults(
                "user-001",
                playlistId,
                List.of("track-alpha"),
                List.of("Artist One"),
                List.of("synth-pop")
            ),
            List.of(),
            List.of(),
            List.of(),
            List.of()
        );
    }
}
