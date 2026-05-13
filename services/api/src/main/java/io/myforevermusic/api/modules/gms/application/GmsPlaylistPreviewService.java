package io.myforevermusic.api.modules.gms.application;

import io.myforevermusic.api.modules.auth.application.AuthAccountStore;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedPlaylistEntity;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedPlaylistRepository;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedPlaylistTrackRepository;
import io.myforevermusic.api.modules.pms.application.PmsUserLibraryStore;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * GMS playlist 후보 흐름의 1차 service.
 * 사용자 PMS 라이브러리가 준비된(baseline 이상) 사용자에 한해 EMS 본 테이블에 적재된
 * 공개 playlist 중 affinity 가 있는 후보를 골라 반환한다.
 *
 * 1차 단계의 affinity 는 단순 — 사용자 PMS user_library 의 distinct artist 집합과
 * EMS playlist 의 curator/title/source 일치로 가중치를 줌. 정교한 6축 evaluator/
 * SASRec 적용은 후속 commit.
 */
@Service
public class GmsPlaylistPreviewService {

    private static final int MAX_LIMIT = 50;

    private final AuthAccountStore authAccountStore;
    private final PmsUserLibraryStore pmsUserLibraryStore;
    private final EmsCollectedPlaylistRepository playlistRepository;
    private final EmsCollectedPlaylistTrackRepository playlistTrackRepository;

    public GmsPlaylistPreviewService(
        AuthAccountStore authAccountStore,
        PmsUserLibraryStore pmsUserLibraryStore,
        EmsCollectedPlaylistRepository playlistRepository,
        EmsCollectedPlaylistTrackRepository playlistTrackRepository
    ) {
        this.authAccountStore = authAccountStore;
        this.pmsUserLibraryStore = pmsUserLibraryStore;
        this.playlistRepository = playlistRepository;
        this.playlistTrackRepository = playlistTrackRepository;
    }

    public GmsPlaylistPreviewResult preview(String userId, Integer limit) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "user_id is required.");
        }

        long pmsTrackCount = pmsUserLibraryStore.findPlaylists(userId).stream()
            .mapToLong(playlist -> playlist.trackCount())
            .sum();
        if (pmsTrackCount == 0L) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "GMS playlist preview requires an imported PMS user library (current stage = cold-start)."
            );
        }

        String preferredPlatform = authAccountStore.findByUserId(userId)
            .map(account -> account.preferredPlatformId())
            .filter(value -> value != null && !value.isBlank())
            .orElse(null);

        int safeLimit = Math.max(1, Math.min(MAX_LIMIT, limit == null ? 12 : limit));

        List<EmsCollectedPlaylistEntity> source;
        if (preferredPlatform != null) {
            source = playlistRepository.findBySourcePlatformOrderByCollectedAtDesc(preferredPlatform, safeLimit * 3);
        } else {
            source = playlistRepository.findAll();
        }

        Set<String> userArtists = collectUserArtists(userId);

        List<GmsPlaylistPreviewCandidate> candidates = source.stream()
            .map(playlist -> scoreCandidate(playlist, userArtists))
            .filter(candidate -> candidate.affinityScore() > 0.0d)
            .sorted((left, right) -> Double.compare(right.affinityScore(), left.affinityScore()))
            .limit(safeLimit)
            .toList();

        return new GmsPlaylistPreviewResult(
            userId,
            preferredPlatform,
            pmsTrackCount > 0L ? "baseline" : "cold-start",
            Instant.now(),
            candidates
        );
    }

    private Set<String> collectUserArtists(String userId) {
        Set<String> result = new HashSet<>();
        pmsUserLibraryStore.findPlaylists(userId).forEach(playlist ->
            playlist.tracks().forEach(track -> {
                if (track.artistName() != null && !track.artistName().isBlank()) {
                    result.add(track.artistName().trim().toLowerCase(Locale.ROOT));
                }
            })
        );
        return result;
    }

    private GmsPlaylistPreviewCandidate scoreCandidate(
        EmsCollectedPlaylistEntity playlist,
        Set<String> userArtists
    ) {
        long linkedTrackCount = playlistTrackRepository.countTracksByPlaylistId(playlist.getId());
        long filledFeatureCount = playlistTrackRepository.countAudioFeatureFilledTracksByPlaylistId(playlist.getId());

        double baseAffinity = userArtists.contains(normalize(playlist.getCurator())) ? 0.45d : 0.15d;
        if (titleMatchesUserArtists(playlist.getTitle(), userArtists)) {
            baseAffinity += 0.15d;
        }
        double coverageBonus = linkedTrackCount == 0L
            ? 0.0d
            : Math.min(0.30d, ((double) filledFeatureCount / linkedTrackCount) * 0.30d);
        double sizeBonus = Math.min(0.10d, linkedTrackCount / 80.0d);
        double affinity = Math.min(1.0d, baseAffinity + coverageBonus + sizeBonus);

        double confidence = linkedTrackCount == 0L
            ? 0.0d
            : Math.min(1.0d, 0.5d + ((double) filledFeatureCount / Math.max(1, linkedTrackCount)) * 0.5d);

        return new GmsPlaylistPreviewCandidate(
            playlist.getId(),
            playlist.getExternalPlaylistId(),
            playlist.getSourcePlatform(),
            playlist.getTitle(),
            playlist.getCurator(),
            playlist.getDescription(),
            playlist.getCoverImageUrl(),
            playlist.getPlatformExternalUrl(),
            linkedTrackCount,
            filledFeatureCount,
            round(affinity),
            round(confidence),
            playlist.getCollectedAt()
        );
    }

    private boolean titleMatchesUserArtists(String title, Set<String> userArtists) {
        if (title == null || title.isBlank() || userArtists.isEmpty()) {
            return false;
        }
        String normalized = title.toLowerCase(Locale.ROOT);
        return userArtists.stream().anyMatch(normalized::contains);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private double round(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }

    public record GmsPlaylistPreviewCandidate(
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
        Instant collectedAt
    ) {}

    public record GmsPlaylistPreviewResult(
        String userId,
        String preferredPlatform,
        String modelStage,
        Instant generatedAt,
        List<GmsPlaylistPreviewCandidate> candidates
    ) {}
}
