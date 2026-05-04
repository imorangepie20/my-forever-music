package io.myforevermusic.api.modules.pms.presentation;

import io.myforevermusic.api.modules.pms.application.PmsWorkspaceBootstrapService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pms/workspace")
public class PmsWorkspaceBootstrapController {

    private final PmsWorkspaceBootstrapService pmsWorkspaceBootstrapService;

    public PmsWorkspaceBootstrapController(PmsWorkspaceBootstrapService pmsWorkspaceBootstrapService) {
        this.pmsWorkspaceBootstrapService = pmsWorkspaceBootstrapService;
    }

    @Operation(summary = "Get PMS bootstrap data for the workspace UI")
    @GetMapping("/bootstrap")
    public PmsWorkspaceBootstrapResponse getWorkspaceBootstrap(
        @RequestParam(name = "user_id", required = false) String userId,
        @RequestParam(name = "playlist_id", required = false) String playlistId
    ) {
        return pmsWorkspaceBootstrapService.getWorkspaceBootstrap(userId, playlistId);
    }
}
