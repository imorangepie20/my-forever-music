package io.myforevermusic.api.modules.ems.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.myforevermusic.api.modules.ems.application.EmsCollectionService;
import io.myforevermusic.api.modules.ems.application.EmsCollectionService.EmsCollectionSearchResult;
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

    public EmsCollectionController(EmsCollectionService emsCollectionService) {
        this.emsCollectionService = emsCollectionService;
    }

    @Operation(summary = "Search Spotify for public playlists and collect results into EMS")
    @PostMapping("/search")
    public EmsCollectionSearchResponse search(@Valid @RequestBody EmsCollectionSearchRequest request) {
        EmsCollectionSearchResult result = emsCollectionService.collectFromSearch(
            request.userId(),
            request.platformId(),
            request.query(),
            request.limit() != null ? request.limit() : 10
        );
        return new EmsCollectionSearchResponse(
            "api", "ems_search_completed", Instant.now(),
            result.platformId(), result.query(),
            result.collectedPlaylistCount(), result.collectedTrackCount(),
            result.collectedAt()
        );
    }

    @Operation(summary = "Browse collected EMS playlists")
    @GetMapping("/playlists")
    public EmsCollectionPlaylistBrowseResponse browsePlaylists(
        @RequestParam(defaultValue = "spotify") String platformId
    ) {
        List<EmsCollectedPlaylistEntity> playlists = emsCollectionService.getCollectedPlaylists(platformId);
        return new EmsCollectionPlaylistBrowseResponse(
            "api", "ok", Instant.now(), platformId,
            playlists.stream().map(p -> new EmsCollectionPlaylistItem(
                p.getId(), p.getExternalPlaylistId(), p.getTitle(),
                p.getSourcePlatform(), p.getCurator(), p.getDescription(),
                p.getCoverImageUrl(), p.getPlatformExternalUrl(), p.getSpotifyUri(),
                p.getTrackCount(), p.getCollectionSource(), p.getSearchQuery(),
                p.getCollectedAt()
            )).toList()
        );
    }

    @Operation(summary = "Get tracks for a collected EMS playlist")
    @GetMapping("/playlists/{playlistId}/tracks")
    public EmsCollectionTrackBrowseResponse getPlaylistTracks(@PathVariable Long playlistId) {
        List<EmsCollectedTrackEntity> tracks = emsCollectionService.getTracksForPlaylist(playlistId);
        return new EmsCollectionTrackBrowseResponse(
            "api", "ok", Instant.now(), playlistId,
            tracks.stream().map(t -> new EmsCollectionTrackItem(
                t.getId(), t.getExternalTrackId(), t.getTitle(),
                t.getArtistName(), t.getSourcePlatform(), t.getAlbumTitle(),
                t.getAlbumImageUrl(), t.getPlatformExternalUrl(), t.getSpotifyUri(),
                t.getPreviewUrl(), t.getDurationMs(), t.getCollectedAt()
            )).toList()
        );
    }

    @Operation(summary = "Browse collected EMS tracks")
    @GetMapping("/tracks")
    public EmsCollectionTrackBrowseResponse browseTracks(
        @RequestParam(defaultValue = "spotify") String platformId
    ) {
        List<EmsCollectedTrackEntity> tracks = emsCollectionService.getCollectedTracks(platformId);
        return new EmsCollectionTrackBrowseResponse(
            "api", "ok", Instant.now(), null,
            tracks.stream().map(t -> new EmsCollectionTrackItem(
                t.getId(), t.getExternalTrackId(), t.getTitle(),
                t.getArtistName(), t.getSourcePlatform(), t.getAlbumTitle(),
                t.getAlbumImageUrl(), t.getPlatformExternalUrl(), t.getSpotifyUri(),
                t.getPreviewUrl(), t.getDurationMs(), t.getCollectedAt()
            )).toList()
        );
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EmsCollectionSearchRequest(
        @NotBlank String userId,
        @NotBlank String platformId,
        @NotBlank String query,
        Integer limit
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EmsCollectionSearchResponse(
        String service, String status, Instant generatedAt,
        String platformId, String query,
        int collectedPlaylistCount, int collectedTrackCount,
        Instant collectedAt
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EmsCollectionPlaylistBrowseResponse(
        String service, String status, Instant generatedAt,
        String platformId,
        List<EmsCollectionPlaylistItem> playlists
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EmsCollectionPlaylistItem(
        Long id, String externalPlaylistId, String title,
        String sourcePlatform, String curator, String description,
        String coverImageUrl, String platformExternalUrl, String spotifyUri,
        int trackCount, String collectionSource, String searchQuery,
        Instant collectedAt
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
        String artistName, String sourcePlatform, String albumTitle,
        String albumImageUrl, String platformExternalUrl, String spotifyUri,
        String previewUrl, Integer durationMs, Instant collectedAt
    ) {}
}
