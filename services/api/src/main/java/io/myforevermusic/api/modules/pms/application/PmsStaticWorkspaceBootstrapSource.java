package io.myforevermusic.api.modules.pms.application;

import io.myforevermusic.api.modules.pms.presentation.PmsWorkspaceBootstrapResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class PmsStaticWorkspaceBootstrapSource implements PmsWorkspaceBootstrapSource {

    @Override
    public Optional<PmsWorkspaceBootstrapResponse> load(String userId, String playlistId) {
        return Optional.of(
            new PmsWorkspaceBootstrapResponse(
                "api",
                "ok",
                Instant.now(),
                new PmsWorkspaceBootstrapResponse.WorkspaceDefaults(
                    userId == null ? "" : userId,
                    playlistId == null ? "" : playlistId,
                    List.of(),
                    List.of(),
                    List.of()
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of()
            )
        );
    }
}
