package io.myforevermusic.api.modules.recommendation.application;

import io.myforevermusic.api.modules.recommendation.infrastructure.ai.AiSasrecTrainingClient;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 주기적으로 SASRec MVP 모델을 자동 학습/promote 한다.
 *
 * 기본 disabled. enable 하려면:
 *   app.recommendation.sasrec.auto-train.enabled=true
 *
 * 동작 모드:
 * - app.recommendation.sasrec.auto-train.user-id 가 지정되면 그 user 한 명만 학습 대상이다.
 * - 비어 있으면 최근 active-window-hours(기본 168h=7일) 이내 user_music_event 가 있는
 *   사용자(최대 max-active-users 명)를 자동 추출해 각자 학습한다.
 *
 * Drift 감지:
 * - 시간 기반: fixed-delay-ms 주기로 tick 발생 (기본 24시간)
 * - 이벤트 수 기반: 마지막 학습 이후 새 event 수가 min-event-delta(기본 50) 이상일 때만
 *   학습. 학습 이력이 없는 사용자는 첫 학습이 매번 통과한다.
 *
 * 학습 이력은 in-memory map 으로만 추적한다. 서버 재시작 시 reset → 첫 tick 에서 모든
 * active user 가 한 번씩 학습된다. 영속 저장은 다음 단계 작업.
 */
@Component
public class SasrecAutoTrainScheduler {

    private static final Logger log = LoggerFactory.getLogger(SasrecAutoTrainScheduler.class);

    private final SasrecModelRegistryAdminService adminService;
    private final UserMusicEventStore eventStore;

    private final Map<String, TrainState> lastTrainStateByUser = new ConcurrentHashMap<>();

    @Value("${app.recommendation.sasrec.auto-train.enabled:false}")
    private boolean enabled;

    @Value("${app.recommendation.sasrec.auto-train.user-id:}")
    private String userId;

    @Value("${app.recommendation.sasrec.auto-train.active-window-hours:168}")
    private int activeWindowHours;

    @Value("${app.recommendation.sasrec.auto-train.max-active-users:10}")
    private int maxActiveUsers;

    @Value("${app.recommendation.sasrec.auto-train.min-event-delta:50}")
    private int minEventDelta;

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

    public SasrecAutoTrainScheduler(
        SasrecModelRegistryAdminService adminService,
        UserMusicEventStore eventStore
    ) {
        this.adminService = adminService;
        this.eventStore = eventStore;
    }

    @Scheduled(
        fixedDelayString = "${app.recommendation.sasrec.auto-train.fixed-delay-ms:86400000}",
        initialDelayString = "${app.recommendation.sasrec.auto-train.initial-delay-ms:600000}"
    )
    public void run() {
        if (!enabled) {
            return;
        }
        Instant now = Instant.now();
        List<String> targets = resolveTargetUserIds(now);
        if (targets.isEmpty()) {
            log.info("SASRec auto-train tick produced 0 target users. Skipping.");
            return;
        }
        AiSasrecTrainingClient.SasrecTrainingOptions options = buildTrainingOptions();
        for (String targetUserId : targets) {
            try {
                if (!shouldTrain(targetUserId, now)) {
                    continue;
                }
                RecommendationModelTrainingService.AutoTrainResult result = adminService.autoTrainAndPromote(
                    targetUserId,
                    eventLimit > 0 ? eventLimit : null,
                    snapshotLimit > 0 ? snapshotLimit : null,
                    options
                );
                long eventCountAtTrain = countEventsForUser(targetUserId, now);
                lastTrainStateByUser.put(targetUserId, new TrainState(now, eventCountAtTrain));
                log.info(
                    "SASRec auto-train tick user={} qualified={} promoted={} model={} summary={}",
                    targetUserId,
                    result.qualified(),
                    result.promoteResult() != null,
                    result.training().modelVersion(),
                    result.summary()
                );
            } catch (Exception ex) {
                log.warn("SASRec auto-train scheduler failed for user={}: {}", targetUserId, ex.getMessage());
            }
        }
    }

    private List<String> resolveTargetUserIds(Instant now) {
        if (userId != null && !userId.isBlank()) {
            return List.of(userId);
        }
        Instant since = now.minus(Duration.ofHours(Math.max(1, activeWindowHours)));
        try {
            return eventStore.findActiveUserIds(since, Math.max(1, maxActiveUsers));
        } catch (Exception ex) {
            log.warn("SASRec auto-train failed to resolve active users: {}", ex.getMessage());
            return List.of();
        }
    }

    private boolean shouldTrain(String targetUserId, Instant now) {
        TrainState previous = lastTrainStateByUser.get(targetUserId);
        if (previous == null) {
            return true;
        }
        try {
            long delta = eventStore.countEventsByUserIdAfter(targetUserId, previous.trainedAt());
            if (delta >= Math.max(1, minEventDelta)) {
                return true;
            }
            log.info(
                "SASRec auto-train skip user={} (delta={} < threshold={}, last_trained_at={})",
                targetUserId,
                delta,
                minEventDelta,
                previous.trainedAt()
            );
            return false;
        } catch (Exception ex) {
            log.warn("SASRec auto-train drift check failed for user={}: {}", targetUserId, ex.getMessage());
            return false;
        }
    }

    private long countEventsForUser(String targetUserId, Instant now) {
        try {
            Instant epoch = Instant.EPOCH;
            return eventStore.countEventsByUserIdAfter(targetUserId, epoch);
        } catch (Exception ex) {
            log.warn("SASRec auto-train failed to count events for user={}: {}", targetUserId, ex.getMessage());
            return 0L;
        }
    }

    private AiSasrecTrainingClient.SasrecTrainingOptions buildTrainingOptions() {
        return new AiSasrecTrainingClient.SasrecTrainingOptions(
            maxContextLength,
            k,
            epochs,
            hiddenSize,
            learningRate,
            true
        );
    }

    private record TrainState(Instant trainedAt, long eventCountAtTrain) {}
}
