package io.myforevermusic.api.modules.recommendation.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DriftSignalEvaluatorTest {

    @Test
    void shouldRaiseSignalsForLowPmsAudioPlaybackAndIsrcCoverage() {
        DriftSignalEvaluator evaluator = configuredEvaluator();
        FeatureCoverageAdminService.FeatureCoverageReport report = report(
            new FeatureCoverageAdminService.PmsLibraryCoverage(1, 100L, 20L, 0.2d, 30L, 0.3d, 50L, 0.5d),
            emptyEms(),
            new FeatureCoverageAdminService.LearningDataCoverage(100L, 0L, 1000)
        );

        List<DriftSignalEvaluator.DriftSignal> signals = evaluator.evaluate(report);

        assertThat(signals).extracting(DriftSignalEvaluator.DriftSignal::category)
            .containsExactlyInAnyOrder(
                DriftSignalEvaluator.CATEGORY_PMS_AUDIO,
                DriftSignalEvaluator.CATEGORY_PMS_PLAYBACK,
                DriftSignalEvaluator.CATEGORY_PMS_ISRC
            );
        assertThat(signals).allSatisfy(signal ->
            assertThat(signal.severity()).isEqualTo(DriftSignalEvaluator.SEVERITY_WARN)
        );
    }

    @Test
    void shouldRespectMinTrackCountGuardForEmsSources() {
        DriftSignalEvaluator evaluator = configuredEvaluator();
        FeatureCoverageAdminService.FeatureCoverageReport report = report(
            new FeatureCoverageAdminService.PmsLibraryCoverage(0, 0L, 0L, 0d, 0L, 0d, 0L, 0d),
            new FeatureCoverageAdminService.EmsPoolCoverage(
                10L,
                0L,
                0d,
                0L,
                0d,
                0L,
                0d,
                List.of(new FeatureCoverageAdminService.EmsSourceCoverage(
                    "tidal",
                    10L,
                    0L,
                    0d,
                    0L,
                    0d,
                    0L,
                    0d
                )),
                List.of()
            ),
            new FeatureCoverageAdminService.LearningDataCoverage(1000L, 0L, 1000)
        );

        // 10 tracks is below the ems-min-track-count (20) so no source-level signals should fire
        assertThat(evaluator.evaluate(report)).isEmpty();
    }

    @Test
    void shouldRaiseEmsAudioAndIsrcSignalsAboveMinTrackCount() {
        DriftSignalEvaluator evaluator = configuredEvaluator();
        FeatureCoverageAdminService.FeatureCoverageReport report = report(
            new FeatureCoverageAdminService.PmsLibraryCoverage(0, 0L, 0L, 0d, 0L, 0d, 0L, 0d),
            new FeatureCoverageAdminService.EmsPoolCoverage(
                30L,
                3L,
                0.1d,
                6L,
                0.2d,
                0L,
                0d,
                List.of(new FeatureCoverageAdminService.EmsSourceCoverage(
                    "spotify",
                    30L,
                    3L,
                    0.1d,
                    6L,
                    0.2d,
                    0L,
                    0d
                )),
                List.of()
            ),
            new FeatureCoverageAdminService.LearningDataCoverage(1000L, 0L, 1000)
        );

        List<DriftSignalEvaluator.DriftSignal> signals = evaluator.evaluate(report);
        assertThat(signals).extracting(DriftSignalEvaluator.DriftSignal::category)
            .contains(
                DriftSignalEvaluator.CATEGORY_EMS_AUDIO,
                DriftSignalEvaluator.CATEGORY_EMS_ISRC
            );
        assertThat(signals).extracting(DriftSignalEvaluator.DriftSignal::targetScope)
            .allSatisfy(scope -> assertThat(scope).isEqualTo("ems:spotify"));
    }

    @Test
    void shouldRaiseLearningSignalWhenEventCountBelowThreshold() {
        DriftSignalEvaluator evaluator = configuredEvaluator();
        FeatureCoverageAdminService.FeatureCoverageReport report = report(
            new FeatureCoverageAdminService.PmsLibraryCoverage(0, 0L, 0L, 0d, 0L, 0d, 0L, 0d),
            emptyEms(),
            new FeatureCoverageAdminService.LearningDataCoverage(10L, 0L, 1000)
        );

        List<DriftSignalEvaluator.DriftSignal> signals = evaluator.evaluate(report);
        assertThat(signals).extracting(DriftSignalEvaluator.DriftSignal::category)
            .containsExactly(DriftSignalEvaluator.CATEGORY_LEARNING_THIN);
        assertThat(signals).extracting(DriftSignalEvaluator.DriftSignal::severity)
            .containsExactly(DriftSignalEvaluator.SEVERITY_INFO);
    }

    @Test
    void shouldEmitNothingWhenAllMetricsHealthy() {
        DriftSignalEvaluator evaluator = configuredEvaluator();
        FeatureCoverageAdminService.FeatureCoverageReport report = report(
            new FeatureCoverageAdminService.PmsLibraryCoverage(2, 100L, 90L, 0.9d, 80L, 0.8d, 95L, 0.95d),
            new FeatureCoverageAdminService.EmsPoolCoverage(
                100L,
                90L,
                0.9d,
                85L,
                0.85d,
                70L,
                0.7d,
                List.of(new FeatureCoverageAdminService.EmsSourceCoverage(
                    "spotify",
                    100L,
                    90L,
                    0.9d,
                    85L,
                    0.85d,
                    70L,
                    0.7d
                )),
                List.of()
            ),
            new FeatureCoverageAdminService.LearningDataCoverage(500L, 50L, 1000)
        );

        assertThat(evaluator.evaluate(report)).isEmpty();
    }

    private DriftSignalEvaluator configuredEvaluator() {
        DriftSignalEvaluator evaluator = new DriftSignalEvaluator();
        ReflectionTestUtils.setField(evaluator, "pmsAudioMinRatio", 0.5d);
        ReflectionTestUtils.setField(evaluator, "pmsPlaybackMinRatio", 0.7d);
        ReflectionTestUtils.setField(evaluator, "pmsIsrcMinRatio", 0.4d);
        ReflectionTestUtils.setField(evaluator, "emsAudioMinRatio", 0.3d);
        ReflectionTestUtils.setField(evaluator, "emsIsrcMinRatio", 0.5d);
        ReflectionTestUtils.setField(evaluator, "emsCanonicalMinRatio", 0.2d);
        ReflectionTestUtils.setField(evaluator, "emsMinTrackCount", 20L);
        ReflectionTestUtils.setField(evaluator, "emsCanonicalMinTrackCount", 50L);
        ReflectionTestUtils.setField(evaluator, "learningMinEventCount", 50L);
        return evaluator;
    }

    private FeatureCoverageAdminService.EmsPoolCoverage emptyEms() {
        return new FeatureCoverageAdminService.EmsPoolCoverage(
            0L,
            0L,
            0d,
            0L,
            0d,
            0L,
            0d,
            List.of(),
            List.of()
        );
    }

    private FeatureCoverageAdminService.FeatureCoverageReport report(
        FeatureCoverageAdminService.PmsLibraryCoverage pms,
        FeatureCoverageAdminService.EmsPoolCoverage ems,
        FeatureCoverageAdminService.LearningDataCoverage learning
    ) {
        return new FeatureCoverageAdminService.FeatureCoverageReport(
            "user-1",
            Instant.parse("2026-05-14T00:00:00Z"),
            "ok",
            pms,
            ems,
            learning,
            List.of(),
            List.of()
        );
    }
}
