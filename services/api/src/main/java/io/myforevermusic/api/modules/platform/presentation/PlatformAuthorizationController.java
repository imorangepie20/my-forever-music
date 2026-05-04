package io.myforevermusic.api.modules.platform.presentation;

import io.myforevermusic.api.modules.platform.application.PlatformAuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platforms/oauth")
public class PlatformAuthorizationController {

    private final PlatformAuthorizationService platformAuthorizationService;

    public PlatformAuthorizationController(PlatformAuthorizationService platformAuthorizationService) {
        this.platformAuthorizationService = platformAuthorizationService;
    }

    @Operation(summary = "Start a real provider OAuth authorization flow for a platform")
    @PostMapping("/start")
    public PlatformAuthorizationStartResponse start(@Valid @RequestBody PlatformAuthorizationStartRequest request) {
        return platformAuthorizationService.startAuthorization(request);
    }

    @Operation(summary = "Complete a real provider OAuth authorization flow for a platform")
    @PostMapping("/complete")
    public PlatformAuthorizationCompleteResponse complete(@Valid @RequestBody PlatformAuthorizationCompleteRequest request) {
        return platformAuthorizationService.completeAuthorization(request);
    }
}
