package io.myforevermusic.api.modules.recommendation.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.myforevermusic.api.modules.recommendation.application.MetadataNormalizationAdminService;
import io.myforevermusic.api.modules.recommendation.infrastructure.musicbrainz.MusicBrainzClient.MusicBrainzArtistCredit;
import io.myforevermusic.api.modules.recommendation.infrastructure.musicbrainz.MusicBrainzClient.MusicBrainzRecording;
import io.myforevermusic.api.modules.recommendation.infrastructure.musicbrainz.MusicBrainzClient.MusicBrainzRecordingSearchResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
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
        @RequestParam(value = "limit", defaultValue = "10") int limit
    ) {
        MusicBrainzRecordingSearchResponse response = adminService.lookupMusicBrainz(userId, title, artist, limit);
        List<MetadataLookupCandidate> candidates = Optional.ofNullable(response.recordings())
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
            response.count() == null ? candidates.size() : response.count(),
            candidates
        );
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record MetadataLookupResponse(
        String service,
        String status,
        Instant generatedAt,
        String title,
        String artist,
        int totalCount,
        List<MetadataLookupCandidate> candidates
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
}
