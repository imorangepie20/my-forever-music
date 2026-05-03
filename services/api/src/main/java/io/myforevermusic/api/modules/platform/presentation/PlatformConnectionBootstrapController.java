package io.myforevermusic.api.modules.platform.presentation;

import io.myforevermusic.api.modules.platform.application.PlatformConnectionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platforms/connections")
@Validated
public class PlatformConnectionBootstrapController {

    private final PlatformConnectionService platformConnectionService;

    public PlatformConnectionBootstrapController(PlatformConnectionService platformConnectionService) {
        this.platformConnectionService = platformConnectionService;
    }

    @Operation(summary = "Get platform connection onboarding state for the current user")
    @GetMapping("/bootstrap")
    public PlatformConnectionBootstrapResponse getBootstrap(@RequestParam("user_id") @NotBlank String userId) {
        return platformConnectionService.getBootstrap(userId);
    }

    @Operation(summary = "Connect a platform account in the current onboarding environment")
    @PostMapping("/connect")
    public PlatformConnectionCommandResponse connect(@Valid @RequestBody PlatformConnectRequest request) {
        return platformConnectionService.connect(request);
    }

    @Operation(summary = "Disconnect a platform account in the current onboarding environment")
    @PostMapping("/disconnect")
    public PlatformConnectionCommandResponse disconnect(@Valid @RequestBody PlatformDisconnectRequest request) {
        return platformConnectionService.disconnect(request);
    }
}
