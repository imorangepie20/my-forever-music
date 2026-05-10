package io.myforevermusic.api.modules.ems.presentation;

import io.myforevermusic.api.modules.ems.application.EmsWorkspaceAnalysisService;
import io.myforevermusic.api.modules.ems.application.EmsOverviewService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ems/workspace")
public class EmsWorkspaceAnalysisController {

    private final EmsWorkspaceAnalysisService emsWorkspaceAnalysisService;
    private final EmsOverviewService emsOverviewService;

    public EmsWorkspaceAnalysisController(
        EmsWorkspaceAnalysisService emsWorkspaceAnalysisService,
        EmsOverviewService emsOverviewService
    ) {
        this.emsWorkspaceAnalysisService = emsWorkspaceAnalysisService;
        this.emsOverviewService = emsOverviewService;
    }

    @Operation(summary = "Analyze PMS seeds and recommend an EMS workspace profile")
    @PostMapping("/analysis")
    public EmsWorkspaceAnalysisResponse analyzeWorkspace(@Valid @RequestBody EmsWorkspaceAnalysisRequest request) {
        return emsWorkspaceAnalysisService.analyzeWorkspace(request);
    }

    @Operation(summary = "Build EMS overview from deterministic signals and AI interpretation")
    @PostMapping("/overview")
    public EmsOverviewResponse overview(@Valid @RequestBody EmsOverviewRequest request) {
        return emsOverviewService.getOverview(request);
    }
}
