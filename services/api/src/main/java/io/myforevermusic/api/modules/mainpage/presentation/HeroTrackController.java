package io.myforevermusic.api.modules.mainpage.presentation;

import io.myforevermusic.api.modules.mainpage.application.HeroTrackService;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/main-page")
public class HeroTrackController {

    private final HeroTrackService heroTrackService;

    public HeroTrackController(HeroTrackService heroTrackService) {
        this.heroTrackService = heroTrackService;
    }

    @GetMapping("/hero-track")
    public ResponseEntity<HeroTrackResponse> getHeroTrack(
        @RequestParam(value = "user_id", required = false) String userId
    ) {
        Optional<HeroTrackResponse> response = heroTrackService.resolve(userId);
        return response
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
