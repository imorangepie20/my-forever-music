package io.myforevermusic.api.modules.recommendation.application;

import io.myforevermusic.api.modules.auth.application.AuthAccountStore;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsAcquisitionRunEntity;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsAcquisitionRunRepository;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedTrackRepository;
import io.myforevermusic.api.modules.pms.application.PmsUserLibraryStore;
import io.myforevermusic.api.modules.pms.application.PmsUserLibraryStore.LibraryPlaylistState;
import io.myforevermusic.api.modules.pms.application.PmsUserLibraryStore.LibraryTrackState;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
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
    private final Optional<EmsAcquisitionRunRepository> emsAcquisitionRunRepository;
    private final DriftSignalEvaluator driftSignalEvaluator;

    @Value("${app.recommendation.drift.audio-stale-days:90}")
    private long audioStaleDays = 90L;

    public FeatureCoverageAdminService(
        AuthAccountStore authAccountStore,
        PmsUserLibraryStore pmsUserLibraryStore,
        UserMusicEventStore eventStore,
        RecommendationSnapshotStore snapshotStore,
        Optional<EmsCollectedTrackRepository> emsTrackRepository,
        Optional<EmsAcquisitionRunRepository> emsAcquisitionRunRepository,
        DriftSignalEvaluator driftSignalEvaluator
    ) {
        this.authAccountStore = authAccountStore;
        this.pmsUserLibraryStore = pmsUserLibraryStore;
        this.eventStore = eventStore;
        this.snapshotStore = snapshotStore;
        this.emsTrackRepository = emsTrackRepository;
        this.emsAcquisitionRunRepository = emsAcquisitionRunRepository;
        this.driftSignalEvaluator = driftSignalEvaluator;
    }

    public FeatureCoverageReport summarize(String adminUserId, String targetUserId) {
        assertAdmin(adminUserId);
        String resolvedTargetUserId = targetUserId == null || targetUserId.isBlank()
            ? adminUserId
            : targetUserId.trim();

        Instant generatedAt = Instant.now();
        Instant staleCutoff = generatedAt.minus(Math.max(1L, audioStaleDays), ChronoUnit.DAYS);
        PmsLibraryCoverage pmsCoverage = summarizePmsLibrary(resolvedTargetUserId, staleCutoff);
        EmsPoolCoverage emsCoverage = summarizeEmsPool(staleCutoff);
        EmsAcquisitionCoverage acquisitionCoverage = summarizeAcquisition();
        LearningDataCoverage learningCoverage = new LearningDataCoverage(
            eventStore.countEventsByUserIdAfter(resolvedTargetUserId, Instant.EPOCH),
            snapshotStore.findRecentByUserId(resolvedTargetUserId, RECENT_SNAPSHOT_LIMIT).size(),
            RECENT_SNAPSHOT_LIMIT
        );

        List<String> warnings = new ArrayList<>();
        warnings.addAll(emsCoverage.warnings());
        warnings.addAll(acquisitionCoverage.warnings());

        FeatureCoverageReport draft = new FeatureCoverageReport(
            resolvedTargetUserId,
            generatedAt,
            warnings.isEmpty() ? "ok" : "degraded",
            pmsCoverage,
            emsCoverage,
            acquisitionCoverage,
            learningCoverage,
            warnings,
            List.of()
        );
        List<DriftSignalEvaluator.DriftSignal> driftSignals = driftSignalEvaluator.evaluate(draft);
        String status = warnings.isEmpty() && driftSignals.isEmpty() ? "ok" : "degraded";
        return new FeatureCoverageReport(
            draft.targetUserId(),
            draft.generatedAt(),
            status,
            draft.pmsLibrary(),
            draft.emsPool(),
            draft.emsAcquisition(),
            draft.learningData(),
            draft.warnings(),
            driftSignals
        );
    }

    private PmsLibraryCoverage summarizePmsLibrary(String userId, Instant staleCutoff) {
        List<LibraryPlaylistState> playlists = pmsUserLibraryStore.findPlaylists(userId);
        long trackCount = 0L;
        long audioFeatureFilledCount = 0L;
        long staleAudioFeatureCount = 0L;
        long isrcCount = 0L;
        long playbackTargetAvailableCount = 0L;
        Instant latestAudioResolvedAt = null;

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
                    Instant resolvedAt = track.audioFeatures().getResolvedAt();
                    if (resolvedAt != null && resolvedAt.isBefore(staleCutoff)) {
                        staleAudioFeatureCount++;
                    }
                    latestAudioResolvedAt = latest(latestAudioResolvedAt, resolvedAt);
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
            staleAudioFeatureCount,
            ratio(staleAudioFeatureCount, audioFeatureFilledCount),
            latestAudioResolvedAt,
            isrcCount,
            ratio(isrcCount, trackCount),
            playbackTargetAvailableCount,
            ratio(playbackTargetAvailableCount, trackCount)
        );
    }

    private EmsPoolCoverage summarizeEmsPool(Instant staleCutoff) {
        if (emsTrackRepository.isEmpty()) {
            return new EmsPoolCoverage(0L, 0L, 0.0d, 0L, 0.0d, null, 0L, 0.0d, 0L, 0.0d, List.of(), List.of(
                "EMS coverage is unavailable because the collected track repository is not configured in this profile."
            ));
        }

        List<EmsSourceCoverage> sources = emsTrackRepository.get().summarizeFeatureCoverageBySourcePlatform(staleCutoff).stream()
            .map(row -> {
                long trackCount = value(row.getTrackCount());
                long audioFeatureFilledCount = value(row.getAudioFeatureFilledCount());
                long staleAudioFeatureCount = value(row.getStaleAudioFeatureCount());
                long isrcCount = value(row.getIsrcCount());
                long canonicalTrackCount = value(row.getCanonicalTrackCount());
                return new EmsSourceCoverage(
                    hasText(row.getSourcePlatform()) ? row.getSourcePlatform() : "unknown",
                    trackCount,
                    audioFeatureFilledCount,
                    ratio(audioFeatureFilledCount, trackCount),
                    staleAudioFeatureCount,
                    ratio(staleAudioFeatureCount, audioFeatureFilledCount),
                    row.getLatestAudioResolvedAt(),
                    isrcCount,
                    ratio(isrcCount, trackCount),
                    canonicalTrackCount,
                    ratio(canonicalTrackCount, trackCount)
                );
            })
            .toList();

        long trackCount = sources.stream().mapToLong(EmsSourceCoverage::trackCount).sum();
        long audioFeatureFilledCount = sources.stream().mapToLong(EmsSourceCoverage::audioFeatureFilledCount).sum();
        long staleAudioFeatureCount = sources.stream().mapToLong(EmsSourceCoverage::staleAudioFeatureCount).sum();
        Instant latestAudioResolvedAt = sources.stream()
            .map(EmsSourceCoverage::latestAudioResolvedAt)
            .filter(value -> value != null)
            .max(Comparator.naturalOrder())
            .orElse(null);
        long isrcCount = sources.stream().mapToLong(EmsSourceCoverage::isrcCount).sum();
        long canonicalTrackCount = sources.stream().mapToLong(EmsSourceCoverage::canonicalTrackCount).sum();

        return new EmsPoolCoverage(
            trackCount,
            audioFeatureFilledCount,
            ratio(audioFeatureFilledCount, trackCount),
            staleAudioFeatureCount,
            ratio(staleAudioFeatureCount, audioFeatureFilledCount),
            latestAudioResolvedAt,
            isrcCount,
            ratio(isrcCount, trackCount),
            canonicalTrackCount,
            ratio(canonicalTrackCount, trackCount),
            sources,
            List.of()
        );
    }

    private EmsAcquisitionCoverage summarizeAcquisition() {
        if (emsAcquisitionRunRepository.isEmpty()) {
            return new EmsAcquisitionCoverage(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0.0d, List.of(
                "EMS acquisition coverage is unavailable because the acquisition run repository is not configured in this profile."
            ));
        }

        List<EmsAcquisitionRunEntity> runs = emsAcquisitionRunRepository.get().findTop20ByOrderByStartedAtDesc();
        long articleCount = runs.stream().mapToLong(EmsAcquisitionRunEntity::getArticleCount).sum();
        long skippedArticleCount = runs.stream().mapToLong(EmsAcquisitionRunEntity::getSkippedArticleCount).sum();
        long seedCount = runs.stream().mapToLong(EmsAcquisitionRunEntity::getSeedCount).sum();
        long skippedSeedCount = runs.stream().mapToLong(EmsAcquisitionRunEntity::getSkippedSeedCount).sum();
        long checkedItemCount = articleCount + seedCount + skippedSeedCount;
        long skippedItemCount = skippedArticleCount + skippedSeedCount;

        return new EmsAcquisitionCoverage(
            runs.size(),
            articleCount,
            skippedArticleCount,
            seedCount,
            skippedSeedCount,
            checkedItemCount,
            skippedItemCount,
            ratio(skippedItemCount, checkedItemCount),
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

    private static Instant latest(Instant current, Instant candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null || candidate.isAfter(current)) {
            return candidate;
        }
        return current;
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
        EmsAcquisitionCoverage emsAcquisition,
        LearningDataCoverage learningData,
        List<String> warnings,
        List<DriftSignalEvaluator.DriftSignal> driftSignals
    ) {}

    public record PmsLibraryCoverage(
        int playlistCount,
        long trackCount,
        long audioFeatureFilledCount,
        double audioFeatureCoverageRatio,
        long staleAudioFeatureCount,
        double staleAudioFeatureRatio,
        Instant latestAudioResolvedAt,
        long isrcCount,
        double isrcCoverageRatio,
        long playbackTargetAvailableCount,
        double playbackTargetCoverageRatio
    ) {}

    public record EmsPoolCoverage(
        long trackCount,
        long audioFeatureFilledCount,
        double audioFeatureCoverageRatio,
        long staleAudioFeatureCount,
        double staleAudioFeatureRatio,
        Instant latestAudioResolvedAt,
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
        long staleAudioFeatureCount,
        double staleAudioFeatureRatio,
        Instant latestAudioResolvedAt,
        long isrcCount,
        double isrcCoverageRatio,
        long canonicalTrackCount,
        double canonicalTrackCoverageRatio
    ) {}

    public record EmsAcquisitionCoverage(
        long recentRunCount,
        long articleCount,
        long skippedArticleCount,
        long seedCount,
        long skippedSeedCount,
        long checkedItemCount,
        long skippedItemCount,
        double skippedItemRatio,
        List<String> warnings
    ) {}

    public record LearningDataCoverage(
        long eventCount,
        long recentRecommendationSnapshotCount,
        int recentRecommendationSnapshotLimit
    ) {}
}
