package io.myforevermusic.api.modules.platform.presentation;

import io.myforevermusic.api.modules.platform.application.SpotifyPlaybackTargetResolverService;
import io.myforevermusic.api.modules.platform.application.SpotifyPlaybackTargetResolverService.SpotifyPlaybackTarget;
import io.myforevermusic.api.modules.platform.application.SpotifyPlaybackTargetResolverService.TrackQuery;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platforms/playback/spotify")
@Validated
public class SpotifyPlaybackTargetController {

    private final SpotifyPlaybackTargetResolverService resolverService;

    public SpotifyPlaybackTargetController(SpotifyPlaybackTargetResolverService resolverService) {
        this.resolverService = resolverService;
    }

    @Operation(summary = "Resolve non-Spotify track metadata to a playable Spotify track target")
    @PostMapping("/resolve-track")
    public SpotifyPlaybackTargetResolveResponse resolveTrack(
        @Valid @RequestBody SpotifyPlaybackTargetResolveRequest request
    ) {
        SpotifyPlaybackTarget target = resolverService.resolve(
            request.userId(),
            new TrackQuery(
                request.title(),
                request.artistName(),
                request.sourcePlatform(),
                request.externalTrackId(),
                request.platformUri(),
                request.tidalTrackId(),
                request.isrc(),
                request.durationMs()
            )
        );

        return new SpotifyPlaybackTargetResolveResponse(
            "api",
            "ok",
            Instant.now(),
            request.userId(),
            request.sourcePlatform(),
            request.externalTrackId(),
            target.spotifyTrackId(),
            target.spotifyUri(),
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
