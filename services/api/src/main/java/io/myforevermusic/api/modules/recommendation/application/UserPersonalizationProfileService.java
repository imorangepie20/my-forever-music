package io.myforevermusic.api.modules.recommendation.application;

import io.myforevermusic.api.modules.auth.application.AuthAccountStore;
import io.myforevermusic.api.modules.recommendation.application.UserMusicEventStore.StoredEvent;
import io.myforevermusic.api.modules.recommendation.application.UserPersonalizationProfileStore.ArtistAffinity;
import io.myforevermusic.api.modules.recommendation.application.UserPersonalizationProfileStore.PlatformAffinity;
import io.myforevermusic.api.modules.recommendation.application.UserPersonalizationProfileStore.Profile;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 사용자별 가벼운 개인화 프로필을 최근 user_music_event 에서 재계산한다.
 *
 * Why (Phase 5 sub-item 1): 전체 모델 재학습 없이도 새 행동(저장/스킵/반복)이 다음 추천 batch에
 * 반영되려면, 모델 외부에 "지금 이 사용자가 어떤 artist/source에 무겁게 반응했는지"를 빠르게
 * 조회할 수 있는 신호가 필요하다. SASRec sequence encoder 갱신은 비싸지만, 이 프로필은 가볍게
 * 자주 갱신할 수 있다. 후속 sub-item 2(recent session reranking)와 4(cold-start fallback)가
 * 이 프로필을 입력으로 사용한다.
 */
@Service
public class UserPersonalizationProfileService {

    private static final String ADMIN_EMAIL = "jowoosungtidal@gmail.com";

    private final AuthAccountStore authAccountStore;
    private final UserMusicEventStore eventStore;
    private final UserPersonalizationProfileStore profileStore;

    @Value("${app.recommendation.personalization.profile-event-limit:200}")
    private int eventLimit;

    @Value("${app.recommendation.personalization.top-artist-limit:10}")
    private int topArtistLimit;

    @Value("${app.recommendation.personalization.top-platform-limit:5}")
    private int topPlatformLimit;

    public UserPersonalizationProfileService(
        AuthAccountStore authAccountStore,
        UserMusicEventStore eventStore,
        UserPersonalizationProfileStore profileStore
    ) {
        this.authAccountStore = authAccountStore;
        this.eventStore = eventStore;
        this.profileStore = profileStore;
    }

    /**
     * Admin 호출용: 다른 사용자의 프로필을 재계산한다.
     */
    public RecomputeResult recomputeForAdmin(String adminUserId, String targetUserId, Integer eventLimitOverride) {
        assertAdmin(adminUserId);
        String resolvedTargetUserId = resolveTargetUserId(adminUserId, targetUserId);
        return recomputeInternal(resolvedTargetUserId, eventLimitOverride);
    }

    /**
     * Admin 호출용: 다른 사용자의 현재 프로필을 조회한다 (재계산 없음).
     */
    public Optional<Profile> findProfileForAdmin(String adminUserId, String targetUserId) {
        assertAdmin(adminUserId);
        String resolvedTargetUserId = resolveTargetUserId(adminUserId, targetUserId);
        return profileStore.findByUserId(resolvedTargetUserId);
    }

    /**
     * 내부용: 권한 검사 없이 한 사용자의 프로필만 재계산한다. 후속 sub-item에서 reranker가 호출.
     */
    public RecomputeResult recompute(String userId) {
        return recomputeInternal(userId, null);
    }

    private RecomputeResult recomputeInternal(String userId, Integer eventLimitOverride) {
        int safeLimit = Math.max(1, Math.min(1000, eventLimitOverride == null ? eventLimit : eventLimitOverride));
        List<StoredEvent> recent = eventStore.findRecentByUserId(userId, safeLimit);

        Map<String, AffinityAggregate> artistAggregates = new HashMap<>();
        Map<String, AffinityAggregate> platformAggregates = new HashMap<>();
        Instant lastEventAt = null;
        long countedSignals = 0L;

        for (StoredEvent event : recent) {
            double weight = signalWeight(event);
            if (weight == 0.0d) {
                continue;
            }
            countedSignals++;
            if (event.artistName() != null && !event.artistName().isBlank()) {
                aggregate(artistAggregates, event.artistName().trim(), weight);
            }
            if (event.sourcePlatform() != null && !event.sourcePlatform().isBlank()) {
                aggregate(platformAggregates, event.sourcePlatform().trim().toLowerCase(Locale.ROOT), weight);
            }
            if (event.occurredAt() != null && (lastEventAt == null || event.occurredAt().isAfter(lastEventAt))) {
                lastEventAt = event.occurredAt();
            }
        }

        List<ArtistAffinity> topArtists = artistAggregates.values().stream()
            .sorted((a, b) -> Double.compare(b.score, a.score))
            .limit(Math.max(1, topArtistLimit))
            .map(agg -> new ArtistAffinity(agg.label, round(agg.score), agg.signalCount))
            .toList();
        List<PlatformAffinity> topPlatforms = platformAggregates.values().stream()
            .sorted((a, b) -> Double.compare(b.score, a.score))
            .limit(Math.max(1, topPlatformLimit))
            .map(agg -> new PlatformAffinity(agg.label, round(agg.score), agg.signalCount))
            .toList();

        Instant now = Instant.now();
        Profile saved = profileStore.upsert(new UserPersonalizationProfileStore.Draft(
            userId,
            topArtists,
            topPlatforms,
            countedSignals,
            lastEventAt,
            now
        ));
        return new RecomputeResult(saved, recent.size(), countedSignals, safeLimit);
    }

    private void aggregate(Map<String, AffinityAggregate> bucket, String labelInput, double weight) {
        String key = labelInput.toLowerCase(Locale.ROOT);
        AffinityAggregate agg = bucket.get(key);
        if (agg == null) {
            agg = new AffinityAggregate(labelInput);
            bucket.put(key, agg);
        }
        agg.score += weight;
        agg.signalCount++;
    }

    /**
     * Event type 별 신호 강도. eventWeight 가 있으면 우선 사용하고, 없으면 기본값을 부여한다.
     * 부정 신호(skip, reject)는 음수로 반영된다.
     */
    private double signalWeight(StoredEvent event) {
        if (event.eventWeight() != null) {
            return event.eventWeight();
        }
        String type = event.eventType() == null ? "" : event.eventType().trim().toLowerCase(Locale.ROOT);
        return switch (type) {
            case "track_saved", "added_to_playlist", "repeat_played", "recommendation_saved" -> 1.5d;
            case "recommendation_liked" -> 1.0d;
            case "play_completed" -> 0.7d;
            case "playlist_imported" -> 0.3d;
            case "stopped_midway" -> -0.3d;
            case "skipped_early" -> -0.5d;
            case "recommendation_rejected" -> -1.0d;
            default -> 0.0d;
        };
    }

    private void assertAdmin(String userId) {
        String normalizedEmail = authAccountStore.findByUserId(userId)
            .map(account -> account.normalizedEmail())
            .orElse("");
        if (!ADMIN_EMAIL.equals(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Personalization profile admin access is restricted.");
        }
    }

    private String resolveTargetUserId(String adminUserId, String targetUserId) {
        if (targetUserId == null || targetUserId.isBlank()) {
            return adminUserId;
        }
        return targetUserId.trim();
    }

    private double round(double value) {
        return Math.round(value * 10000.0d) / 10000.0d;
    }

    public record RecomputeResult(
        Profile profile,
        int eventsScanned,
        long signalCount,
        int eventLimit
    ) {}

    private static final class AffinityAggregate {
        final String label;
        double score;
        long signalCount;

        AffinityAggregate(String label) {
            this.label = label;
        }
    }
}
