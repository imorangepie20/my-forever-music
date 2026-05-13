package io.myforevermusic.api.modules.gms.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.myforevermusic.api.modules.gms.application.GmsPlaylistPreviewService;
import io.myforevermusic.api.modules.gms.application.GmsPlaylistPreviewService.GmsPlaylistPreviewCandidate;
import io.myforevermusic.api.modules.gms.application.GmsPlaylistPreviewService.GmsPlaylistPreviewResult;
import io.myforevermusic.api.modules.gms.application.GmsPlaylistPreviewService.SaveResult;
import io.myforevermusic.api.modules.recommendation.application.AxisEvidence;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1/gms/playlists")
public class GmsPlaylistPreviewController {

    private final GmsPlaylistPreviewService service;

    public GmsPlaylistPreviewController(GmsPlaylistPreviewService service) {
        this.service = service;
    }

    @Operation(summary = "List GMS playlist candidates resolved from EMS collected playlists for the requested user")
    @GetMapping("/preview")
    public GmsPlaylistPreviewResponse preview(
        @RequestParam("user_id") String userId,
        @RequestParam(value = "limit", required = false) Integer limit
    ) {
        GmsPlaylistPreviewResult result = service.preview(userId, limit);
        return GmsPlaylistPreviewResponse.from(result);
    }

    @Operation(summary = "Save an EMS playlist into the user's PMS personal playlist library")
    @PostMapping("/{playlistId}/save")
    public GmsPlaylistSaveResponse save(
        @PathVariable Long playlistId,
        @RequestParam("user_id") String userId,
        @RequestBody(required = false) GmsPlaylistSaveRequest request
    ) {
        SaveResult result = service.saveToPms(
            userId,
            playlistId,
            request == null ? null : request.title()
        );
        return GmsPlaylistSaveResponse.from(result);
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record GmsPlaylistSaveRequest(String title) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record GmsPlaylistSaveResponse(
        String service,
        String status,
        Instant generatedAt,
        String userId,
        Long emsPlaylistId,
        String personalPlaylistId,
        String personalPlaylistTitle,
        int personalPlaylistTrackCount,
        int addedTrackCount,
        Instant savedAt
    ) {
        static GmsPlaylistSaveResponse from(SaveResult result) {
            return new GmsPlaylistSaveResponse(
                "api",
                "ok",
                Instant.now(),
                result.userId(),
                result.emsPlaylistId(),
                result.personalPlaylistId(),
                result.personalPlaylistTitle(),
                result.personalPlaylistTrackCount(),
                result.addedTrackCount(),
                result.savedAt()
            );
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record GmsPlaylistPreviewResponse(
        String service,
        String status,
        Instant generatedAt,
        String userId,
        String preferredPlatform,
        String modelStage,
        List<GmsPlaylistPreviewItem> candidates
    ) {
        static GmsPlaylistPreviewResponse from(GmsPlaylistPreviewResult result) {
            return new GmsPlaylistPreviewResponse(
                "api",
                "ok",
                result.generatedAt(),
                result.userId(),
                result.preferredPlatform(),
                result.modelStage(),
                result.candidates().stream().map(GmsPlaylistPreviewItem::from).toList()
            );
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record GmsPlaylistPreviewItem(
        Long playlistId,
        String externalPlaylistId,
        String sourcePlatform,
        String title,
        String curator,
        String description,
        String coverImageUrl,
        String platformExternalUrl,
        long trackCount,
        long audioFeatureFilledCount,
        double affinityScore,
        double confidenceScore,
        double compositeScore,
        Instant collectedAt,
        List<AxisEvidence> axisEvidence
    ) {
        static GmsPlaylistPreviewItem from(GmsPlaylistPreviewCandidate candidate) {
            return new GmsPlaylistPreviewItem(
                candidate.playlistId(),
                candidate.externalPlaylistId(),
                candidate.sourcePlatform(),
                candidate.title(),
                candidate.curator(),
                candidate.description(),
                candidate.coverImageUrl(),
                candidate.platformExternalUrl(),
                candidate.trackCount(),
                candidate.audioFeatureFilledCount(),
                candidate.affinityScore(),
                candidate.confidenceScore(),
                candidate.compositeScore(),
                candidate.collectedAt(),
                candidate.axisEvidence()
            );
        }
    }
}
