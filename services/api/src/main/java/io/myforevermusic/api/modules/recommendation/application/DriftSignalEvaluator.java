package io.myforevermusic.api.modules.recommendation.application;

import io.myforevermusic.api.modules.recommendation.application.FeatureCoverageAdminService.EmsSourceCoverage;
import io.myforevermusic.api.modules.recommendation.application.FeatureCoverageAdminService.FeatureCoverageReport;
import io.myforevermusic.api.modules.recommendation.application.FeatureCoverageAdminService.LearningDataCoverage;
import io.myforevermusic.api.modules.recommendation.application.FeatureCoverageAdminService.PmsLibraryCoverage;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Feature coverage 보고서를 입력으로 받아 사전 정의 임계치를 어긴 항목을 drift signal로 변환한다.
 *
 * Why: 운영자가 매번 모든 비율을 직접 비교하지 않아도, 임계치 미달 항목만 모아 한 곳에 보여주기 위함.
 * 임계치는 @Value 로 외부 설정 가능 (기본값은 코드에 포함). EMS source 단위 신호는 표본 크기가 너무 작으면
 * (예: track 5개) 무의미하므로 minimum track count 가드를 둔다.
 */
@Component
public class DriftSignalEvaluator {

    static final String SEVERITY_WARN = "warn";
    static final String SEVERITY_INFO = "info";

    static final String CATEGORY_PMS_AUDIO = "pms_audio";
    static final String CATEGORY_PMS_PLAYBACK = "pms_playback";
    static final String CATEGORY_PMS_ISRC = "pms_isrc";
    static final String CATEGORY_EMS_AUDIO = "ems_audio";
    static final String CATEGORY_EMS_ISRC = "ems_isrc";
    static final String CATEGORY_EMS_CANONICAL = "ems_canonical";
    static final String CATEGORY_LEARNING_THIN = "learning_data";

    @Value("${app.recommendation.drift.pms-audio-min-ratio:0.5}")
    private double pmsAudioMinRatio;

    @Value("${app.recommendation.drift.pms-playback-min-ratio:0.7}")
    private double pmsPlaybackMinRatio;

    @Value("${app.recommendation.drift.pms-isrc-min-ratio:0.4}")
    private double pmsIsrcMinRatio;

    @Value("${app.recommendation.drift.ems-audio-min-ratio:0.3}")
    private double emsAudioMinRatio;

    @Value("${app.recommendation.drift.ems-isrc-min-ratio:0.5}")
    private double emsIsrcMinRatio;

    @Value("${app.recommendation.drift.ems-canonical-min-ratio:0.2}")
    private double emsCanonicalMinRatio;

    @Value("${app.recommendation.drift.ems-min-track-count:20}")
    private long emsMinTrackCount;

    @Value("${app.recommendation.drift.ems-canonical-min-track-count:50}")
    private long emsCanonicalMinTrackCount;

    @Value("${app.recommendation.drift.learning-min-event-count:50}")
    private long learningMinEventCount;

    public List<DriftSignal> evaluate(FeatureCoverageReport report) {
        if (report == null) {
            return List.of();
        }
        List<DriftSignal> signals = new ArrayList<>();
        evaluatePms(report.pmsLibrary(), signals);
        evaluateEms(report.emsPool() == null ? null : report.emsPool().sources(), signals);
        evaluateLearning(report.learningData(), signals);
        return signals;
    }

    private void evaluatePms(PmsLibraryCoverage pms, List<DriftSignal> signals) {
        if (pms == null || pms.trackCount() <= 0L) {
            return;
        }
        if (pms.audioFeatureCoverageRatio() < pmsAudioMinRatio) {
            signals.add(new DriftSignal(
                CATEGORY_PMS_AUDIO,
                SEVERITY_WARN,
                "pms",
                "PMS audio feature coverage가 임계치(%.0f%%) 미만(%.0f%%, %d/%d)".formatted(
                    pmsAudioMinRatio * 100.0d,
                    pms.audioFeatureCoverageRatio() * 100.0d,
                    pms.audioFeatureFilledCount(),
                    pms.trackCount()
                ),
                pms.audioFeatureCoverageRatio(),
                pmsAudioMinRatio,
                pms.trackCount()
            ));
        }
        if (pms.playbackTargetCoverageRatio() < pmsPlaybackMinRatio) {
            signals.add(new DriftSignal(
                CATEGORY_PMS_PLAYBACK,
                SEVERITY_WARN,
                "pms",
                "PMS playback target 보유율이 임계치(%.0f%%) 미만(%.0f%%, %d/%d)".formatted(
                    pmsPlaybackMinRatio * 100.0d,
                    pms.playbackTargetCoverageRatio() * 100.0d,
                    pms.playbackTargetAvailableCount(),
                    pms.trackCount()
                ),
                pms.playbackTargetCoverageRatio(),
                pmsPlaybackMinRatio,
                pms.trackCount()
            ));
        }
        if (pms.isrcCoverageRatio() < pmsIsrcMinRatio) {
            signals.add(new DriftSignal(
                CATEGORY_PMS_ISRC,
                SEVERITY_WARN,
                "pms",
                "PMS ISRC coverage가 임계치(%.0f%%) 미만(%.0f%%, %d/%d)".formatted(
                    pmsIsrcMinRatio * 100.0d,
                    pms.isrcCoverageRatio() * 100.0d,
                    pms.isrcCount(),
                    pms.trackCount()
                ),
                pms.isrcCoverageRatio(),
                pmsIsrcMinRatio,
                pms.trackCount()
            ));
        }
    }

    private void evaluateEms(List<EmsSourceCoverage> sources, List<DriftSignal> signals) {
        if (sources == null) {
            return;
        }
        for (EmsSourceCoverage source : sources) {
            if (source == null) {
                continue;
            }
            if (source.trackCount() >= emsMinTrackCount) {
                String scope = "ems:" + source.sourcePlatform();
                if (source.audioFeatureCoverageRatio() < emsAudioMinRatio) {
                    signals.add(new DriftSignal(
                        CATEGORY_EMS_AUDIO,
                        SEVERITY_WARN,
                        scope,
                        "EMS %s audio feature coverage가 임계치(%.0f%%) 미만(%.0f%%, %d/%d)".formatted(
                            source.sourcePlatform(),
                            emsAudioMinRatio * 100.0d,
                            source.audioFeatureCoverageRatio() * 100.0d,
                            source.audioFeatureFilledCount(),
                            source.trackCount()
                        ),
                        source.audioFeatureCoverageRatio(),
                        emsAudioMinRatio,
                        source.trackCount()
                    ));
                }
                if (source.isrcCoverageRatio() < emsIsrcMinRatio) {
                    signals.add(new DriftSignal(
                        CATEGORY_EMS_ISRC,
                        SEVERITY_WARN,
                        scope,
                        "EMS %s ISRC coverage가 임계치(%.0f%%) 미만(%.0f%%, %d/%d)".formatted(
                            source.sourcePlatform(),
                            emsIsrcMinRatio * 100.0d,
                            source.isrcCoverageRatio() * 100.0d,
                            source.isrcCount(),
                            source.trackCount()
                        ),
                        source.isrcCoverageRatio(),
                        emsIsrcMinRatio,
                        source.trackCount()
                    ));
                }
            }
            if (source.trackCount() >= emsCanonicalMinTrackCount
                && source.canonicalTrackCoverageRatio() < emsCanonicalMinRatio) {
                signals.add(new DriftSignal(
                    CATEGORY_EMS_CANONICAL,
                    SEVERITY_INFO,
                    "ems:" + source.sourcePlatform(),
                    "EMS %s canonical track 연결률이 임계치(%.0f%%) 미만(%.0f%%, %d/%d) — Phase 2 normalize 진행 필요".formatted(
                        source.sourcePlatform(),
                        emsCanonicalMinRatio * 100.0d,
                        source.canonicalTrackCoverageRatio() * 100.0d,
                        source.canonicalTrackCount(),
                        source.trackCount()
                    ),
                    source.canonicalTrackCoverageRatio(),
                    emsCanonicalMinRatio,
                    source.trackCount()
                ));
            }
        }
    }

    private void evaluateLearning(LearningDataCoverage learning, List<DriftSignal> signals) {
        if (learning == null) {
            return;
        }
        if (learning.eventCount() < learningMinEventCount) {
            signals.add(new DriftSignal(
                CATEGORY_LEARNING_THIN,
                SEVERITY_INFO,
                "learning",
                "사용자 user_music_event가 학습 임계치(%d) 미만(%d) — 모델 재학습 신호가 부족할 수 있음".formatted(
                    learningMinEventCount,
                    learning.eventCount()
                ),
                null,
                null,
                learning.eventCount()
            ));
        }
    }

    public record DriftSignal(
        String category,
        String severity,
        String targetScope,
        String message,
        Double actualValue,
        Double threshold,
        long sampleSize
    ) {}
}
