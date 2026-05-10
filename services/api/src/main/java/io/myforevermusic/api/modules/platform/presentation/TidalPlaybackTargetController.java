package io.myforevermusic.api.modules.platform.presentation;

import io.myforevermusic.api.modules.platform.application.TidalPlaybackTargetResolverService;
import io.myforevermusic.api.modules.platform.application.TidalPlaybackTargetResolverService.TrackQuery;
import io.myforevermusic.api.modules.platform.application.TidalPlaybackTargetResolverService.TidalPlaybackTarget;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platforms/playback/tidal")
@Validated
public class TidalPlaybackTargetController {

    private final TidalPlaybackTargetResolverService resolverService;

    public TidalPlaybackTargetController(TidalPlaybackTargetResolverService resolverService) {
        this.resolverService = resolverService;
    }

    @Operation(summary = "Resolve non-TIDAL track metadata to a playable TIDAL track target")
    @PostMapping("/resolve-track")
    public TidalPlaybackTargetResolveResponse resolveTrack(
        @Valid @RequestBody TidalPlaybackTargetResolveRequest request
    ) {
        TidalPlaybackTarget target = resolverService.resolve(
            request.userId(),
            new TrackQuery(
                request.title(),
                request.artistName(),
                request.sourcePlatform(),
                request.externalTrackId(),
                request.platformUri(),
                request.spotifyTrackId(),
                request.isrc(),
                request.durationMs()
            )
        );

        return new TidalPlaybackTargetResolveResponse(
            "api",
            "ok",
            Instant.now(),
            request.userId(),
            request.sourcePlatform(),
            request.externalTrackId(),
            target.tidalTrackId(),
            target.tidalUri(),
            target.title(),
            target.artistName(),
            target.albumTitle(),
            target.albumImageUrl(),
            target.platformExternalUrl(),
            target.previewUrl(),
            target.isrc(),
            target.durationMs(),
            target.matchReason(),
            target.matchScore()
        );
    }
}
