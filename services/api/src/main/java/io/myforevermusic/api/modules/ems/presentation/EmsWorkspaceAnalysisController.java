package io.myforevermusic.api.modules.ems.presentation;

import io.myforevermusic.api.modules.ems.application.EmsWorkspaceAnalysisService;
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

    public EmsWorkspaceAnalysisController(EmsWorkspaceAnalysisService emsWorkspaceAnalysisService) {
        this.emsWorkspaceAnalysisService = emsWorkspaceAnalysisService;
    }

    @Operation(summary = "Analyze PMS seeds and recommend an EMS workspace profile")
    @PostMapping("/analysis")
    public EmsWorkspaceAnalysisResponse analyzeWorkspace(@Valid @RequestBody EmsWorkspaceAnalysisRequest request) {
        return emsWorkspaceAnalysisService.analyzeWorkspace(request);
    }
}
