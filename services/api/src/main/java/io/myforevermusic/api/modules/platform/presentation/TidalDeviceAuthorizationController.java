package io.myforevermusic.api.modules.platform.presentation;

import io.myforevermusic.api.modules.platform.application.TidalDeviceAuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platforms/oauth/tidal/device")
public class TidalDeviceAuthorizationController {

    private final TidalDeviceAuthorizationService tidalDeviceAuthorizationService;

    public TidalDeviceAuthorizationController(TidalDeviceAuthorizationService tidalDeviceAuthorizationService) {
        this.tidalDeviceAuthorizationService = tidalDeviceAuthorizationService;
    }

    @Operation(summary = "Start the TIDAL device-code authorization flow used by the full-track stream boundary")
    @PostMapping("/start")
    public TidalDeviceAuthorizationStartResponse start(@Valid @RequestBody TidalDeviceAuthorizationStartRequest request) {
        return tidalDeviceAuthorizationService.start(request);
    }

    @Operation(summary = "Poll and complete the TIDAL device-code authorization flow")
    @PostMapping("/poll")
    public TidalDeviceAuthorizationPollResponse poll(@Valid @RequestBody TidalDeviceAuthorizationPollRequest request) {
        return tidalDeviceAuthorizationService.poll(request);
    }
}
