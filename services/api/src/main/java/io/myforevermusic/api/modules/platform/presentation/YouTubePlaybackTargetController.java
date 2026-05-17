package io.myforevermusic.api.modules.platform.presentation;

import io.myforevermusic.api.modules.platform.application.YouTubePlaybackTargetResolverService;
import io.myforevermusic.api.modules.platform.application.YouTubePlaybackTargetResolverService.TrackQuery;
import io.myforevermusic.api.modules.platform.application.YouTubePlaybackTargetResolverService.YouTubePlaybackTarget;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platforms/playback/youtube")
@Validated
public class YouTubePlaybackTargetController {

    private final YouTubePlaybackTargetResolverService resolverService;

    public YouTubePlaybackTargetController(YouTubePlaybackTargetResolverService resolverService) {
        this.resolverService = resolverService;
    }

    @Operation(summary = "Resolve track metadata to an embeddable YouTube video target")
    @PostMapping("/resolve-track")
    public YouTubePlaybackTargetResolveResponse resolveTrack(
        @Valid @RequestBody YouTubePlaybackTargetResolveRequest request
    ) {
        YouTubePlaybackTarget target = resolverService.resolve(new TrackQuery(
            request.title(),
            request.artistName(),
            request.sourcePlatform(),
            request.externalTrackId(),
            request.platformUri(),
            request.spotifyTrackId(),
            request.tidalTrackId(),
            request.isrc(),
            request.durationMs(),
            request.excludedVideoIds()
        ));

        return new YouTubePlaybackTargetResolveResponse(
            "api",
            "ok",
            Instant.now(),
            request.userId(),
            request.sourcePlatform(),
            request.externalTrackId(),
            target.youtubeVideoId(),
            target.youtubeUrl(),
            target.title(),
            target.channelTitle(),
            target.thumbnailUrl(),
            target.durationMs(),
            target.matchReason(),
            target.matchScore(),
            target.candidateCount()
        );
    }
}
