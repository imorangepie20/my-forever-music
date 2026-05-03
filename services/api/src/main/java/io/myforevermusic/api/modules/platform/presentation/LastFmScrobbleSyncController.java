package io.myforevermusic.api.modules.platform.presentation;

import io.myforevermusic.api.modules.platform.application.LastFmScrobbleSyncService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platforms/lastfm/scrobbles")
public class LastFmScrobbleSyncController {

    private final LastFmScrobbleSyncService lastFmScrobbleSyncService;

    public LastFmScrobbleSyncController(LastFmScrobbleSyncService lastFmScrobbleSyncService) {
        this.lastFmScrobbleSyncService = lastFmScrobbleSyncService;
    }

    @GetMapping("/bootstrap")
    public LastFmScrobbleBootstrapResponse getBootstrap(@RequestParam("user_id") String userId) {
        return lastFmScrobbleSyncService.getBootstrap(userId);
    }

    @PostMapping("/sync")
    public LastFmScrobbleSyncResponse syncScrobbles(@Valid @RequestBody LastFmScrobbleSyncRequest request) {
        return lastFmScrobbleSyncService.syncScrobbles(request);
    }
}
