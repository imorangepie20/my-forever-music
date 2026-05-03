package io.myforevermusic.api.modules.platform.presentation;

import io.myforevermusic.api.modules.platform.application.LastFmProfileConnectionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platforms/lastfm")
public class LastFmProfileConnectionController {

    private final LastFmProfileConnectionService lastFmProfileConnectionService;

    public LastFmProfileConnectionController(LastFmProfileConnectionService lastFmProfileConnectionService) {
        this.lastFmProfileConnectionService = lastFmProfileConnectionService;
    }

    @PostMapping("/profile")
    public PlatformConnectionCommandResponse connectProfile(@Valid @RequestBody LastFmProfileConnectRequest request) {
        return lastFmProfileConnectionService.connectProfile(request);
    }
}
