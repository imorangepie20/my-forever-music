package io.myforevermusic.api.modules.pms.application;

import io.myforevermusic.api.modules.pms.presentation.PmsWorkspaceBootstrapResponse;
import java.util.Optional;

public interface PmsWorkspaceBootstrapSource {

    Optional<PmsWorkspaceBootstrapResponse> load();
}
