package io.myforevermusic.api.modules.system.presentation;

import io.swagger.v3.oas.annotations.Operation;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    @Operation(summary = "Get bootstrap information for the API service")
    @GetMapping("/info")
    public SystemInfoResponse getSystemInfo() {
        return new SystemInfoResponse(
            "my-forever-music-api",
            "BOOTSTRAP_READY",
            "Spring Boot scaffold is ready for domain modules.",
            Instant.now()
        );
    }
}
