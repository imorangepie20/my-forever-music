package io.myforevermusic.api.modules.recommendation.application;

import io.myforevermusic.api.modules.auth.application.AuthAccountStore;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedTrackRepository;
import io.myforevermusic.api.modules.pms.application.PmsUserLibraryStore;
import io.myforevermusic.api.modules.pms.application.PmsUserLibraryStore.LibraryPlaylistState;
import io.myforevermusic.api.modules.pms.application.PmsUserLibraryStore.LibraryTrackState;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FeatureCoverageAdminService {

    private static final String ADMIN_EMAIL = "jowoosungtidal@gmail.com";
    private static final int RECENT_SNAPSHOT_LIMIT = 1000;

    private final AuthAccountStore authAccountStore;
    private final PmsUserLibraryStore pmsUserLibraryStore;
    private final UserMusicEventStore eventStore;
    private final RecommendationSnapshotStore snapshotStore;
    private final Optional<EmsCollectedTrackRepository> emsTrackRepository;

    public FeatureCoverageAdminService(
        AuthAccountStore authAccountStore,
        PmsUserLibraryStore pmsUserLibraryStore,
        UserMusicEventStore eventStore,
        RecommendationSnapshotStore snapshotStore,
        Optional<EmsCollectedTrackRepository> emsTrackRepository
    ) {
        this.authAccountStore = authAccountStore;
        this.pmsUserLibraryStore = pmsUserLibraryStore;
        this.eventStore = eventStore;
        this.snapshotStore = snapshotStore;
        this.emsTrackRepository = emsTrackRepository;
    }

    public FeatureCoverageReport summarize(String adminUserId, String targetUserId) {
        assertAdmin(adminUserId);
        String resolvedTargetUserId = targetUserId == null || targetUserId.isBlank()
            ? adminUserId
            : targetUserId.trim();

        PmsLibraryCoverage pmsCoverage = summarizePmsLibrary(resolvedTargetUserId);
        EmsPoolCoverage emsCoverage = summarizeEmsPool();
        LearningDataCoverage learningCoverage = new LearningDataCoverage(
            eventStore.countEventsByUserIdAfter(resolvedTargetUserId, Instant.EPOCH),
            snapshotStore.findRecentByUserId(resolvedTargetUserId, RECENT_SNAPSHOT_LIMIT).size(),
            RECENT_SNAPSHOT_LIMIT
        );

        List<String> warnings = new ArrayList<>();
        warnings.addAll(emsCoverage.warnings());

        return new FeatureCoverageReport(
            resolvedTargetUserId,
            Instant.now(),
            warnings.isEmpty() ? "ok" : "degraded",
            pmsCoverage,
            emsCoverage,
            learningCoverage,
            warnings
        );
    }

    private PmsLibraryCoverage summarizePmsLibrary(String userId) {
        List<LibraryPlaylistState> playlists = pmsUserLibraryStore.findPlaylists(userId);
        long trackCount = 0L;
        long audioFeatureFilledCount = 0L;
        long isrcCount = 0L;
        long playbackTargetAvailableCount = 0L;

        for (LibraryPlaylistState playlist : playlists) {
            if (playlist.tracks() == null) {
                continue;
            }
            for (LibraryTrackState track : playlist.tracks()) {
                if (track == null) {
                    continue;
                }
                trackCount++;
                if (track.audioFeatures() != null && track.audioFeatures().isComplete()) {
                    audioFeatureFilledCount++;
                }
                if (hasText(track.isrc())) {
                    isrcCount++;
                }
                if (isPlaybackTargetAvailable(track)) {
                    playbackTargetAvailableCount++;
                }
            }
        }

        return new PmsLibraryCoverage(
            playlists.size(),
            trackCount,
            audioFeatureFilledCount,
            ratio(audioFeatureFilledCount, trackCount),
            isrcCount,
            ratio(isrcCount, trackCount),
            playbackTargetAvailableCount,
            ratio(playbackTargetAvailableCount, trackCount)
        );
    }

    private EmsPoolCoverage summarizeEmsPool() {
        if (emsTrackRepository.isEmpty()) {
            return new EmsPoolCoverage(0L, 0L, 0.0d, 0L, 0.0d, 0L, 0.0d, List.of(), List.of(
                "EMS coverage is unavailable because the collected track repository is not configured in this profile."
            ));
        }

        List<EmsSourceCoverage> sources = emsTrackRepository.get().summarizeFeatureCoverageBySourcePlatform().stream()
            .map(row -> {
                long trackCount = value(row.getTrackCount());
                long audioFeatureFilledCount = value(row.getAudioFeatureFilledCount());
                long isrcCount = value(row.getIsrcCount());
                long canonicalTrackCount = value(row.getCanonicalTrackCount());
                return new EmsSourceCoverage(
                    hasText(row.getSourcePlatform()) ? row.getSourcePlatform() : "unknown",
                    trackCount,
                    audioFeatureFilledCount,
                    ratio(audioFeatureFilledCount, trackCount),
                    isrcCount,
                    ratio(isrcCount, trackCount),
                    canonicalTrackCount,
                    ratio(canonicalTrackCount, trackCount)
                );
            })
            .toList();

        long trackCount = sources.stream().mapToLong(EmsSourceCoverage::trackCount).sum();
        long audioFeatureFilledCount = sources.stream().mapToLong(EmsSourceCoverage::audioFeatureFilledCount).sum();
        long isrcCount = sources.stream().mapToLong(EmsSourceCoverage::isrcCount).sum();
        long canonicalTrackCount = sources.stream().mapToLong(EmsSourceCoverage::canonicalTrackCount).sum();

        return new EmsPoolCoverage(
            trackCount,
            audioFeatureFilledCount,
            ratio(audioFeatureFilledCount, trackCount),
            isrcCount,
            ratio(isrcCount, trackCount),
            canonicalTrackCount,
            ratio(canonicalTrackCount, trackCount),
            sources,
            List.of()
        );
    }

    private boolean isPlaybackTargetAvailable(LibraryTrackState track) {
        String status = track.playbackTargetStatus();
        return "native".equals(status)
            || "resolved".equals(status)
            || hasText(track.spotifyTrackId())
            || hasText(track.tidalTrackId());
    }

    private void assertAdmin(String userId) {
        String normalizedEmail = authAccountStore.findByUserId(userId)
            .map(account -> account.normalizedEmail())
            .orElse("");
        if (!ADMIN_EMAIL.equals(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Feature coverage admin access is restricted.");
        }
    }

    private static long value(Long value) {
        return value == null ? 0L : value;
    }

    private static double ratio(long numerator, long denominator) {
        if (denominator <= 0L) {
            return 0.0d;
        }
        return Math.round((numerator / (double) denominator) * 10000.0d) / 10000.0d;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record FeatureCoverageReport(
        String targetUserId,
        Instant generatedAt,
        String status,
        PmsLibraryCoverage pmsLibrary,
        EmsPoolCoverage emsPool,
        LearningDataCoverage learningData,
        List<String> warnings
    ) {}

    public record PmsLibraryCoverage(
        int playlistCount,
        long trackCount,
        long audioFeatureFilledCount,
        double audioFeatureCoverageRatio,
        long isrcCount,
        double isrcCoverageRatio,
        long playbackTargetAvailableCount,
        double playbackTargetCoverageRatio
    ) {}

    public record EmsPoolCoverage(
        long trackCount,
        long audioFeatureFilledCount,
        double audioFeatureCoverageRatio,
        long isrcCount,
        double isrcCoverageRatio,
        long canonicalTrackCount,
        double canonicalTrackCoverageRatio,
        List<EmsSourceCoverage> sources,
        List<String> warnings
    ) {}

    public record EmsSourceCoverage(
        String sourcePlatform,
        long trackCount,
        long audioFeatureFilledCount,
        double audioFeatureCoverageRatio,
        long isrcCount,
        double isrcCoverageRatio,
        long canonicalTrackCount,
        double canonicalTrackCoverageRatio
    ) {}

    public record LearningDataCoverage(
        long eventCount,
        long recentRecommendationSnapshotCount,
        int recentRecommendationSnapshotLimit
    ) {}
}
