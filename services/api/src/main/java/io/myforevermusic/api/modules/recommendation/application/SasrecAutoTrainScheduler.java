package io.myforevermusic.api.modules.recommendation.application;

import io.myforevermusic.api.modules.recommendation.infrastructure.ai.AiSasrecTrainingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 주기적으로 admin user 의 SASRec MVP 모델을 학습시키고 qualification=true 일 때 자동 promote 한다.
 *
 * 기본 disabled. enable 하려면:
 *   app.recommendation.sasrec.auto-train.enabled=true
 *   app.recommendation.sasrec.auto-train.user-id=<admin-user-id>
 *   app.recommendation.sasrec.auto-train.fixed-delay-ms=86400000  # 24h, optional
 *
 * 이번 1차 단계는 시간 기반 drift 만 사용한다 (interval 마다 1회 학습).
 * event 수 기반 drift 와 활성 사용자 자동 추출은 후속 단계로 분리되어 있다.
 */
@Component
public class SasrecAutoTrainScheduler {

    private static final Logger log = LoggerFactory.getLogger(SasrecAutoTrainScheduler.class);

    private final SasrecModelRegistryAdminService adminService;

    @Value("${app.recommendation.sasrec.auto-train.enabled:false}")
    private boolean enabled;

    @Value("${app.recommendation.sasrec.auto-train.user-id:}")
    private String userId;

    @Value("${app.recommendation.sasrec.auto-train.event-limit:0}")
    private int eventLimit;

    @Value("${app.recommendation.sasrec.auto-train.snapshot-limit:0}")
    private int snapshotLimit;

    @Value("${app.recommendation.sasrec.auto-train.max-context-length:32}")
    private int maxContextLength;

    @Value("${app.recommendation.sasrec.auto-train.k:10}")
    private int k;

    @Value("${app.recommendation.sasrec.auto-train.epochs:30}")
    private int epochs;

    @Value("${app.recommendation.sasrec.auto-train.hidden-size:32}")
    private int hiddenSize;

    @Value("${app.recommendation.sasrec.auto-train.learning-rate:0.01}")
    private double learningRate;

    public SasrecAutoTrainScheduler(SasrecModelRegistryAdminService adminService) {
        this.adminService = adminService;
    }

    @Scheduled(
        fixedDelayString = "${app.recommendation.sasrec.auto-train.fixed-delay-ms:86400000}",
        initialDelayString = "${app.recommendation.sasrec.auto-train.initial-delay-ms:600000}"
    )
    public void run() {
        if (!enabled) {
            return;
        }
        if (userId == null || userId.isBlank()) {
            log.warn("SASRec auto-train scheduler is enabled but app.recommendation.sasrec.auto-train.user-id is blank. Skipping.");
            return;
        }
        try {
            AiSasrecTrainingClient.SasrecTrainingOptions options = new AiSasrecTrainingClient.SasrecTrainingOptions(
                maxContextLength,
                k,
                epochs,
                hiddenSize,
                learningRate,
                true
            );
            RecommendationModelTrainingService.AutoTrainResult result = adminService.autoTrainAndPromote(
                userId,
                eventLimit > 0 ? eventLimit : null,
                snapshotLimit > 0 ? snapshotLimit : null,
                options
            );
            log.info(
                "SASRec auto-train tick user={} qualified={} promoted={} model={} summary={}",
                userId,
                result.qualified(),
                result.promoteResult() != null,
                result.training().modelVersion(),
                result.summary()
            );
        } catch (Exception ex) {
            log.warn("SASRec auto-train scheduler failed for user={}: {}", userId, ex.getMessage());
        }
    }
}
