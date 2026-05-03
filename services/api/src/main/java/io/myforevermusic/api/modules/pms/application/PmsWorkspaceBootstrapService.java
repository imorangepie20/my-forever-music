package io.myforevermusic.api.modules.pms.application;

import io.myforevermusic.api.modules.pms.presentation.PmsWorkspaceBootstrapResponse;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class PmsWorkspaceBootstrapService {

    private final List<PmsWorkspaceBootstrapSource> sources;

    public PmsWorkspaceBootstrapService(List<PmsWorkspaceBootstrapSource> sources) {
        this.sources = List.copyOf(sources);
    }

    public PmsWorkspaceBootstrapResponse getWorkspaceBootstrap(String userId) {
        return sources.stream()
            .map(source -> source.load(userId))
            .flatMap(Optional::stream)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No PMS workspace bootstrap source is available."));
    }
}
