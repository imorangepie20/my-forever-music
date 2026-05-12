package io.myforevermusic.api.modules.recommendation.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.myforevermusic.api.modules.recommendation.application.MetadataNormalizationAdminService;
import io.myforevermusic.api.modules.recommendation.application.MetadataNormalizationAdminService.LookupResult;
import io.myforevermusic.api.modules.recommendation.application.TrackIdentityCandidateStore;
import io.myforevermusic.api.modules.recommendation.infrastructure.musicbrainz.MusicBrainzClient.MusicBrainzArtistCredit;
import io.myforevermusic.api.modules.recommendation.infrastructure.musicbrainz.MusicBrainzClient.MusicBrainzRecording;
import io.swagger.v3.oas.annotations.Operation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recommendations/admin/metadata")
public class MetadataNormalizationAdminController {

    private final MetadataNormalizationAdminService adminService;

    public MetadataNormalizationAdminController(MetadataNormalizationAdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(summary = "Look up MusicBrainz recording candidates by title + artist for the configured admin user")
    @GetMapping("/musicbrainz/recordings")
    public MetadataLookupResponse lookupMusicBrainzRecordings(
        @RequestParam("user_id") String userId,
        @RequestParam("title") String title,
        @RequestParam(value = "artist", required = false) String artist,
        @RequestParam(value = "limit", defaultValue = "10") int limit,
        @RequestParam(value = "persist", defaultValue = "false") boolean persist
    ) {
        LookupResult result = adminService.lookupMusicBrainz(userId, title, artist, limit, persist);
        List<MetadataLookupCandidate> candidates = Optional.ofNullable(result.response().recordings())
            .orElse(List.of())
            .stream()
            .map(MetadataLookupCandidate::from)
            .toList();
        return new MetadataLookupResponse(
            "api",
            "ok",
            Instant.now(),
            title,
            artist,
            result.response().count() == null ? candidates.size() : result.response().count(),
            candidates,
            result.savedCandidates().stream().map(CandidateItem::from).toList()
        );
    }

    @Operation(summary = "List recent track identity candidates (filter by status)")
    @GetMapping("/candidates")
    public CandidateListResponse listCandidates(
        @RequestParam("user_id") String userId,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "limit", defaultValue = "30") int limit
    ) {
        List<TrackIdentityCandidateStore.Entry> entries = adminService.listCandidates(userId, status, limit);
        return new CandidateListResponse(
            "api",
            "ok",
            Instant.now(),
            entries.stream().map(CandidateItem::from).toList()
        );
    }

    @Operation(summary = "Accept a track identity candidate")
    @PostMapping("/candidates/{candidateId}/accept")
    public CandidateCommandResponse acceptCandidate(
        @PathVariable Long candidateId,
        @RequestParam("user_id") String userId,
        @RequestBody(required = false) CandidateResolutionRequest request
    ) {
        TrackIdentityCandidateStore.Entry entry = adminService.acceptCandidate(
            userId,
            candidateId,
            request == null ? null : request.notes()
        );
        return new CandidateCommandResponse("api", "ok", Instant.now(), CandidateItem.from(entry));
    }

    @Operation(summary = "Reject a track identity candidate")
    @PostMapping("/candidates/{candidateId}/reject")
    public CandidateCommandResponse rejectCandidate(
        @PathVariable Long candidateId,
        @RequestParam("user_id") String userId,
        @RequestBody(required = false) CandidateResolutionRequest request
    ) {
        TrackIdentityCandidateStore.Entry entry = adminService.rejectCandidate(
            userId,
            candidateId,
            request == null ? null : request.notes()
        );
        return new CandidateCommandResponse("api", "ok", Instant.now(), CandidateItem.from(entry));
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record MetadataLookupResponse(
        String service,
        String status,
        Instant generatedAt,
        String title,
        String artist,
        int totalCount,
        List<MetadataLookupCandidate> candidates,
        List<CandidateItem> savedCandidates
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record MetadataLookupCandidate(
        String mbid,
        String title,
        String artistName,
        Integer lengthMs,
        Integer score,
        List<String> isrcs,
        List<String> releaseTitles
    ) {
        static MetadataLookupCandidate from(MusicBrainzRecording recording) {
            String artistName = Optional.ofNullable(recording.artistCredit())
                .map(credits -> String.join(", ", credits.stream().map(MusicBrainzArtistCredit::name).toList()))
                .orElse(null);
            List<String> releaseTitles = Optional.ofNullable(recording.releases())
                .map(releases -> releases.stream().map(ref -> ref.title()).toList())
                .orElse(List.of());
            return new MetadataLookupCandidate(
                recording.id(),
                recording.title(),
                artistName,
                recording.length(),
                recording.score(),
                recording.isrcs() == null ? List.of() : recording.isrcs(),
                releaseTitles
            );
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CandidateListResponse(
        String service,
        String status,
        Instant generatedAt,
        List<CandidateItem> candidates
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CandidateCommandResponse(
        String service,
        String status,
        Instant generatedAt,
        CandidateItem candidate
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CandidateItem(
        Long id,
        String queryTitle,
        String queryArtist,
        String source,
        String candidateKind,
        String candidateValue,
        Double candidateScore,
        String status,
        String createdBy,
        Instant createdAt,
        String resolvedBy,
        Instant resolvedAt,
        String notes
    ) {
        static CandidateItem from(TrackIdentityCandidateStore.Entry entry) {
            return new CandidateItem(
                entry.id(),
                entry.queryTitle(),
                entry.queryArtist(),
                entry.source(),
                entry.candidateKind(),
                entry.candidateValue(),
                entry.candidateScore(),
                entry.status(),
                entry.createdBy(),
                entry.createdAt(),
                entry.resolvedBy(),
                entry.resolvedAt(),
                entry.notes()
            );
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CandidateResolutionRequest(String notes) {}
}
