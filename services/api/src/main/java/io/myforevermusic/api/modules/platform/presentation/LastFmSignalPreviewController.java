package io.myforevermusic.api.modules.platform.presentation;

import io.myforevermusic.api.modules.platform.application.LastFmSignalPreviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platforms/lastfm")
public class LastFmSignalPreviewController {

    private final LastFmSignalPreviewService lastFmSignalPreviewService;

    public LastFmSignalPreviewController(LastFmSignalPreviewService lastFmSignalPreviewService) {
        this.lastFmSignalPreviewService = lastFmSignalPreviewService;
    }

    @GetMapping("/preview")
    public LastFmSignalPreviewResponse getPreview(
        @RequestParam("username") String username,
        @RequestParam(name = "period", defaultValue = "1month") String period,
        @RequestParam(name = "recent_limit", defaultValue = "8") Integer recentLimit,
        @RequestParam(name = "top_limit", defaultValue = "6") Integer topLimit
    ) {
        return lastFmSignalPreviewService.getPreview(username, period, recentLimit, topLimit);
    }
}
