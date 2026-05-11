package io.myforevermusic.api.modules.ems.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.myforevermusic.api.modules.ems.application.EmsCollectionService;
import io.myforevermusic.api.modules.ems.application.EmsCollectionService.EmsAudioFeatureBackfillResult;
import io.myforevermusic.api.modules.ems.application.EmsCollectionService.EmsAudioFeatureCoverage;
import io.myforevermusic.api.modules.ems.application.EmsCollectionService.EmsCollectionSearchPlaylistTracksPreview;
import io.myforevermusic.api.modules.ems.application.EmsCollectionService.EmsCollectionSearchPreviewResult;
import io.myforevermusic.api.modules.ems.application.EmsPublicPlaylistDiscoveryScheduler;
import io.myforevermusic.api.modules.ems.application.EmsPublicPlaylistDiscoveryScheduler.EmsPublicPlaylistDiscoveryRun;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedPlaylistEntity;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedTrackEntity;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ems/collection")
public class EmsCollectionController {

    private final EmsCollectionService emsCollectionService;
    private final EmsPublicPlaylistDiscoveryScheduler emsPublicPlaylistDiscoveryScheduler;

    public EmsCollectionController(
        EmsCollectionService emsCollectionService,
        EmsPublicPlaylistDiscoveryScheduler emsPublicPlaylistDiscoveryScheduler
    ) {
        this.emsCollectionService = emsCollectionService;
        this.emsPublicPlaylistDiscoveryScheduler = emsPublicPlaylistDiscoveryScheduler;
    }

    @Operation(summary = "Search a provider for public playlists without storing results in EMS")
    @PostMapping("/search")
    public EmsCollectionSearchResponse search(@Valid @RequestBody EmsCollectionSearchRequest request) {
        EmsCollectionSearchPreviewResult result = emsCollectionService.previewSearch(
            request.userId(),
            request.platformId(),
            request.query()
        );
        return new EmsCollectionSearchResponse(
            "api", "ems_search_previewed", Instant.now(),
            result.platformId(), result.query(),
            result.resultPlaylistCount(), result.resultTrackCount(),
            result.playlists().stream()
                .map(playlist -> new EmsCollectionSearchPlaylistItem(
                    playlist.externalPlaylistId(),
                    playlist.title(),
                    playlist.sourcePlatform(),
                    playlist.curator(),
                    playlist.description(),
                    playlist.coverImageUrl(),
                    playlist.platformExternalUrl(),
                    playlist.platformUri(),
                    spotifyUriFor(playlist.sourcePlatform(), playlist.platformUri()),
                    playlist.trackCount()
                ))
                .toList(),
            result.tracks().stream()
                .map(track -> new EmsCollectionSearchTrackItem(
                    track.externalTrackId(),
                    track.title(),
                    track.artistName(),
                    track.sourcePlatform(),
                    track.isrc(),
                    track.albumTitle(),
                    track.albumImageUrl(),
                    track.platformExternalUrl(),
                    track.platformUri(),
                    spotifyUriFor(track.sourcePlatform(), track.platformUri()),
                    track.previewUrl(),
                    track.durationMs()
                ))
                .toList(),
            result.searchedAt()
        );
    }

    @Operation(summary = "Preview tracks for a provider playlist search result without storing results in EMS")
    @GetMapping("/search/playlists/{platformId}/{externalPlaylistId}/tracks")
    public EmsCollectionSearchPlaylistTracksResponse getSearchPlaylistTracks(
        @PathVariable String platformId,
        @PathVariable String externalPlaylistId,
        @RequestParam("user_id") String userId
    ) {
        EmsCollectionSearchPlaylistTracksPreview result = emsCollectionService.getSearchPlaylistTracks(
            userId,
            platformId,
            externalPlaylistId
        );
        return new EmsCollectionSearchPlaylistTracksResponse(
            "api",
            "ok",
            Instant.now(),
            result.platformId(),
            result.externalPlaylistId(),
            result.trackCount(),
            result.tracks().stream()
                .map(track -> new EmsCollectionSearchTrackItem(
                    track.externalTrackId(),
                    track.title(),
                    track.artistName(),
                    track.sourcePlatform(),
                    track.isrc(),
                    track.albumTitle(),
                    track.albumImageUrl(),
                    track.platformExternalUrl(),
                    track.platformUri(),
                    spotifyUriFor(track.sourcePlatform(), track.platformUri()),
                    track.previewUrl(),
                    track.durationMs()
                ))
                .toList(),
            result.searchedAt()
        );
    }

    @Operation(summary = "Run EMS public playlist discovery immediately")
    @PostMapping("/discovery/run")
    public EmsDiscoveryRunResponse runDiscovery(@RequestBody(required = false) EmsDiscoveryRunRequest request) {
        EmsDiscoveryRunRequest safeRequest = request == null ? new EmsDiscoveryRunRequest(null, null, null, null) : request;
        EmsPublicPlaylistDiscoveryRun run = emsPublicPlaylistDiscoveryScheduler.runNow(
            safeRequest.userId(),
            safeRequest.platforms(),
            safeRequest.seedQueries(),
            safeRequest.perQueryLimit()
        );
        return EmsDiscoveryRunResponse.from("api", run);
    }

    @Operation(summary = "Get the latest EMS public playlist discovery run status")
    @GetMapping("/discovery/status")
    public EmsDiscoveryRunResponse getDiscoveryStatus() {
        EmsPublicPlaylistDiscoveryRun run = emsPublicPlaylistDiscoveryScheduler.lastRun();
        if (run == null) {
            return new EmsDiscoveryRunResponse(
                "api",
                "not_run",
                Instant.now(),
                null,
                null,
                null,
                List.of(),
                List.of(),
                0,
                0,
                0,
                List.of(),
                "EMS public playlist discovery has not run in this API process."
            );
        }
        return EmsDiscoveryRunResponse.from("api", run);
    }

    @Operation(summary = "Browse collected EMS playlists for display")
    @GetMapping("/playlists")
    public EmsCollectionPlaylistBrowseResponse browsePlaylists(
        @RequestParam(value = "platform_id", defaultValue = "spotify") String platformId,
        @RequestParam(value = "limit", defaultValue = "12") int limit,
        @RequestParam(value = "random", defaultValue = "true") boolean random
    ) {
        List<EmsCollectedPlaylistEntity> playlists = emsCollectionService.getCollectedPlaylists(platformId, limit, random);
        return new EmsCollectionPlaylistBrowseResponse(
            "api", "ok", Instant.now(), platformId,
            playlists.stream().map(this::toPlaylistItem).toList()
        );
    }

    @Operation(summary = "Get a collected EMS playlist with ordered tracks")
    @GetMapping("/playlists/{playlistId}")
    public EmsCollectionPlaylistDetailResponse getPlaylistDetail(@PathVariable Long playlistId) {
        EmsCollectedPlaylistEntity playlist = emsCollectionService.getCollectedPlaylist(playlistId);
        List<EmsCollectedTrackEntity> tracks = emsCollectionService.getTracksForPlaylist(playlistId);
        return new EmsCollectionPlaylistDetailResponse(
            "api", "ok", Instant.now(),
            toPlaylistItem(playlist),
            tracks.stream().map(EmsCollectionController::toTrackItem).toList()
        );
    }

    @Operation(summary = "Backfill audio features for a collected EMS playlist")
    @PostMapping("/playlists/{playlistId}/audio-features/backfill")
    public EmsAudioFeatureBackfillResponse backfillPlaylistAudioFeatures(@PathVariable Long playlistId) {
        EmsAudioFeatureBackfillResult result = emsCollectionService.backfillAudioFeaturesForPlaylist(playlistId);
        return new EmsAudioFeatureBackfillResponse(
            "api",
            "ok",
            Instant.now(),
            result.playlistId(),
            result.playlistTitle(),
            result.sourcePlatform(),
            result.trackCount(),
            result.filledTrackCountBefore(),
            result.pendingTrackCountBefore(),
            result.eligibleTrackCount(),
            result.missingIsrcTrackCount(),
            result.matchedSnapshotCount(),
            result.updatedTrackCount(),
            result.newlyFilledTrackCount(),
            result.filledTrackCountAfter(),
            result.pendingTrackCountAfter(),
            result.coverageRatioAfter(),
            result.backfilledAt()
        );
    }

    @Operation(summary = "Get tracks for a collected EMS playlist")
    @GetMapping("/playlists/{playlistId}/tracks")
    public EmsCollectionTrackBrowseResponse getPlaylistTracks(@PathVariable Long playlistId) {
        List<EmsCollectedTrackEntity> tracks = emsCollectionService.getTracksForPlaylist(playlistId);
        return new EmsCollectionTrackBrowseResponse(
            "api", "ok", Instant.now(), playlistId,
            tracks.stream().map(EmsCollectionController::toTrackItem).toList()
        );
    }

    @Operation(summary = "Browse collected EMS tracks")
    @GetMapping("/tracks")
    public EmsCollectionTrackBrowseResponse browseTracks(
        @RequestParam(value = "platform_id", defaultValue = "spotify") String platformId
    ) {
        List<EmsCollectedTrackEntity> tracks = emsCollectionService.getCollectedTracks(platformId);
        return new EmsCollectionTrackBrowseResponse(
            "api", "ok", Instant.now(), null,
            tracks.stream().map(EmsCollectionController::toTrackItem).toList()
        );
    }

    private EmsCollectionPlaylistItem toPlaylistItem(EmsCollectedPlaylistEntity playlist) {
        EmsAudioFeatureCoverage coverage = emsCollectionService.getAudioFeatureCoverage(playlist.getId());
        return new EmsCollectionPlaylistItem(
            playlist.getId(), playlist.getExternalPlaylistId(), playlist.getTitle(),
            playlist.getSourcePlatform(), playlist.getCurator(), playlist.getDescription(),
            playlist.getCoverImageUrl(), playlist.getPlatformExternalUrl(), playlist.getSpotifyUri(), spotifyUriFor(playlist.getSourcePlatform(), playlist.getSpotifyUri()),
            playlist.getTrackCount(), playlist.getCollectionSource(), playlist.getSearchQuery(),
            playlist.getCollectedAt(),
            new EmsAudioFeatureCoverageItem(
                coverage.trackCount(),
                coverage.filledTrackCount(),
                coverage.pendingTrackCount(),
                coverage.coverageRatio()
            )
        );
    }

    private static EmsCollectionTrackItem toTrackItem(EmsCollectedTrackEntity track) {
        return new EmsCollectionTrackItem(
            track.getId(), track.getExternalTrackId(), track.getTitle(),
            track.getArtistName(), track.getSourcePlatform(), track.getIsrc(), track.getAlbumTitle(),
            track.getAlbumImageUrl(), track.getPlatformExternalUrl(), track.getSpotifyUri(), spotifyUriFor(track.getSourcePlatform(), track.getSpotifyUri()),
            track.getPreviewUrl(), track.getDurationMs(), track.getCollectedAt(),
            toAudioFeatureItem(track)
        );
    }

    private static String spotifyUriFor(String sourcePlatform, String platformUri) {
        return "spotify".equals(sourcePlatform) ? platformUri : null;
    }

    private static EmsTrackAudioFeatureItem toAudioFeatureItem(EmsCollectedTrackEntity track) {
        if (track.getAudioFeatures() == null) {
            return new EmsTrackAudioFeatureItem(
                null, "unresolved", false, null, null, null, null, null, null, null, null, null, null, null, null
            );
        }
        return new EmsTrackAudioFeatureItem(
            track.getAudioFeatures().getAudioFeatureTrackId(),
            track.getAudioFeatures().getAudioFeatureSource(),
            track.getAudioFeatures().isAudioFeaturesFilled(),
            track.getAudioFeatures().getDurationMs(),
            track.getAudioFeatures().getMusicalKey(),
            track.getAudioFeatures().getMode(),
            track.getAudioFeatures().getAcousticness(),
            track.getAudioFeatures().getDanceability(),
            track.getAudioFeatures().getEnergy(),
            track.getAudioFeatures().getInstrumentalness(),
            track.getAudioFeatures().getLiveness(),
            track.getAudioFeatures().getLoudness(),
            track.getAudioFeatures().getSpeechiness(),
            track.getAudioFeatures().getTempo(),
            track.getAudioFeatures().getValence()
        );
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EmsCollectionSearchRequest(
        @NotBlank String userId,
        String platformId,
        @NotBlank String query
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EmsCollectionSearchResponse(
        String service, String status, Instant generatedAt,
        String platformId, String query,
        int resultPlaylistCount, int resultTrackCount,
        List<EmsCollectionSearchPlaylistItem> playlists,
        List<EmsCollectionSearchTrackItem> tracks,
        Instant searchedAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EmsCollectionSearchPlaylistItem(
        String externalPlaylistId,
        String title,
        String sourcePlatform,
        String curator,
        String description,
        String coverImageUrl,
        String platformExternalUrl,
        String platformUri,
        String spotifyUri,
        int trackCount
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EmsCollectionSearchTrackItem(
        String externalTrackId,
        String title,
        String artistName,
        String sourcePlatform,
        String isrc,
        String albumTitle,
        String albumImageUrl,
        String platformExternalUrl,
        String platformUri,
        String spotifyUri,
        String previewUrl,
        Integer durationMs
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EmsCollectionSearchPlaylistTracksResponse(
        String service,
        String status,
        Instant generatedAt,
        String platformId,
        String externalPlaylistId,
        int trackCount,
        List<EmsCollectionSearchTrackItem> tracks,
        Instant searchedAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EmsDiscoveryRunRequest(
        String userId,
        List<String> platforms,
        List<String> seedQueries,
        Integer perQueryLimit
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EmsDiscoveryRunResponse(
        String service,
        String status,
        Instant generatedAt,
        String trigger,
        Instant startedAt,
        Instant completedAt,
        List<String> platforms,
        List<String> seedQueries,
        int perQueryLimit,
        int collectedPlaylistCount,
        int collectedTrackCount,
        List<EmsDiscoveryFailureItem> failures,
        String message
    ) {
        static EmsDiscoveryRunResponse from(String service, EmsPublicPlaylistDiscoveryRun run) {
            return new EmsDiscoveryRunResponse(
                service,
                run.status(),
                Instant.now(),
                run.trigger(),
                run.startedAt(),
                run.completedAt(),
                run.platforms(),
                run.seedQueries(),
                run.perQueryLimit(),
                run.collectedPlaylistCount(),
                run.collectedTrackCount(),
                run.failures().stream()
                    .map(failure -> new EmsDiscoveryFailureItem(
                        failure.platformId(),
                        failure.query(),
                        failure.message()
                    ))
                    .toList(),
                run.message()
            );
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EmsDiscoveryFailureItem(
        String platformId,
        String query,
        String message
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EmsCollectionPlaylistBrowseResponse(
        String service, String status, Instant generatedAt,
        String platformId,
        List<EmsCollectionPlaylistItem> playlists
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EmsCollectionPlaylistDetailResponse(
        String service, String status, Instant generatedAt,
        EmsCollectionPlaylistItem playlist,
        List<EmsCollectionTrackItem> tracks
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EmsCollectionPlaylistItem(
        Long id, String externalPlaylistId, String title,
        String sourcePlatform, String curator, String description,
        String coverImageUrl, String platformExternalUrl, String platformUri, String spotifyUri,
        int trackCount, String collectionSource, String searchQuery,
        Instant collectedAt,
        EmsAudioFeatureCoverageItem audioFeatureCoverage
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EmsAudioFeatureCoverageItem(
        long trackCount,
        long filledTrackCount,
        long pendingTrackCount,
        double coverageRatio
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EmsAudioFeatureBackfillResponse(
        String service,
        String status,
        Instant generatedAt,
        Long playlistId,
        String playlistTitle,
        String sourcePlatform,
        long trackCount,
        long filledTrackCountBefore,
        long pendingTrackCountBefore,
        int eligibleTrackCount,
        int missingIsrcTrackCount,
        int matchedSnapshotCount,
        int updatedTrackCount,
        int newlyFilledTrackCount,
        long filledTrackCountAfter,
        long pendingTrackCountAfter,
        double coverageRatioAfter,
        Instant backfilledAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EmsCollectionTrackBrowseResponse(
        String service, String status, Instant generatedAt,
        Long playlistId,
        List<EmsCollectionTrackItem> tracks
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EmsCollectionTrackItem(
        Long id, String externalTrackId, String title,
        String artistName, String sourcePlatform, String isrc, String albumTitle,
        String albumImageUrl, String platformExternalUrl, String platformUri, String spotifyUri,
        String previewUrl, Integer durationMs, Instant collectedAt,
        EmsTrackAudioFeatureItem audioFeatures
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EmsTrackAudioFeatureItem(
        String audioFeatureTrackId,
        String audioFeatureSource,
        boolean audioFeaturesFilled,
        Integer durationMs,
        Integer musicalKey,
        Integer mode,
        Double acousticness,
        Double danceability,
        Double energy,
        Double instrumentalness,
        Double liveness,
        Double loudness,
        Double speechiness,
        Double tempo,
        Double valence
    ) {}
}
