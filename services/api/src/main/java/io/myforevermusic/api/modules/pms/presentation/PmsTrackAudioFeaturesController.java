package io.myforevermusic.api.modules.pms.presentation;

import io.myforevermusic.api.modules.pms.application.PmsTrackAudioFeaturesService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pms/tracks")
@Validated
public class PmsTrackAudioFeaturesController {

    private final PmsTrackAudioFeaturesService audioFeaturesService;

    public PmsTrackAudioFeaturesController(PmsTrackAudioFeaturesService audioFeaturesService) {
        this.audioFeaturesService = audioFeaturesService;
    }

    @Operation(summary = "Get ReccoBeats-filled audio features for a track (visualizer envelope source)")
    @GetMapping("/{audio_feature_track_id}/audio-features")
    public PmsTrackAudioFeaturesResponse getAudioFeatures(
        @RequestParam("user_id") @NotBlank String userId,
        @PathVariable("audio_feature_track_id") @NotBlank String audioFeatureTrackId
    ) {
        return audioFeaturesService.getAudioFeatures(userId, audioFeatureTrackId);
    }
}
