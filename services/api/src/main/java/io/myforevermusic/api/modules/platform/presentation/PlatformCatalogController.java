package io.myforevermusic.api.modules.platform.presentation;

import io.myforevermusic.api.modules.platform.application.PlatformCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platforms")
public class PlatformCatalogController {

    private final PlatformCatalogService platformCatalogService;

    public PlatformCatalogController(PlatformCatalogService platformCatalogService) {
        this.platformCatalogService = platformCatalogService;
    }

    @Operation(summary = "Get supported streaming platform catalog and onboarding guidance")
    @GetMapping("/catalog")
    public PlatformCatalogResponse getCatalog() {
        return platformCatalogService.getCatalog();
    }
}
