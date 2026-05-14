package io.myforevermusic.api.modules.recommendation.application;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 사용자 행동(이벤트) 별 가중치의 단일 source of truth.
 *
 * Why (Phase 5 sub-item 3): 이전에는 {@code UserMusicEventService} 가 저장 시 사용하는 가중치 맵과
 * {@code UserPersonalizationProfileService} 의 fallback 가중치 맵이 별도로 정의되어 있어 값도 어휘도
 * 일치하지 않았다 ({@code skip_next} vs {@code skipped_early}, {@code replay} vs {@code repeat_played}).
 * 한 곳에서 canonical event type 과 가중치를 관리하고, 모든 별칭은 canonical 로 매핑된다.
 *
 * 가중치의 의미는 {@code PERSONALIZED_RECOMMENDATION_MODEL_PLAN.md} §4-2 의 "신호 해석" 표를 따른다:
 *
 * - 강한 긍정 (+2.0): track_saved, added_to_playlist, recommendation_liked
 * - 긍정 (+1.5): replay
 * - 긍정 (+1.0): play_completed
 * - 약한 긍정 (+0.3): playlist_imported
 * - neutral (0.0): play_started, play_paused, play_resumed, skip_previous
 * - 약한 부정 (-0.1): ignored_recommendation
 * - 약한 부정 (-0.25): skip_next, stopped_midway
 * - 강한 부정 (-2.0): recommendation_rejected
 *
 * 별칭(alias) 은 외부 호환을 위해 받지만, 내부적으로는 canonical 형태로 정규화된 뒤 가중치를 조회한다.
 */
@Component
public class EventSignalWeights {

    private static final Map<String, Double> CANONICAL_WEIGHTS = Map.ofEntries(
        Map.entry("play_started", 0.0),
        Map.entry("play_paused", 0.0),
        Map.entry("play_resumed", 0.0),
        Map.entry("play_completed", 1.0),
        Map.entry("skip_next", -0.25),
        Map.entry("skip_previous", 0.0),
        Map.entry("replay", 1.5),
        Map.entry("track_saved", 2.0),
        Map.entry("added_to_playlist", 2.0),
        Map.entry("recommendation_liked", 2.0),
        Map.entry("recommendation_rejected", -2.0),
        Map.entry("ignored_recommendation", -0.1),
        Map.entry("stopped_midway", -0.25),
        Map.entry("playlist_imported", 0.3)
    );

    private static final Map<String, String> ALIAS_TO_CANONICAL = Map.ofEntries(
        Map.entry("repeat_played", "replay"),
        Map.entry("skipped_early", "skip_next"),
        Map.entry("recommendation_saved", "track_saved")
    );

    /**
     * @return canonical 가중치. canonical/alias 어느 쪽 키도 매핑되지 않으면 null.
     */
    public Double weightFor(String eventType) {
        String canonical = canonicalOf(eventType);
        if (canonical == null) {
            return null;
        }
        return CANONICAL_WEIGHTS.get(canonical);
    }

    /**
     * 외부 입력을 canonical event type 으로 정규화한다. 알 수 없는 type은 null.
     */
    public String canonicalOf(String eventType) {
        if (eventType == null) {
            return null;
        }
        String normalized = eventType.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return null;
        }
        if (CANONICAL_WEIGHTS.containsKey(normalized)) {
            return normalized;
        }
        return ALIAS_TO_CANONICAL.get(normalized);
    }

    /**
     * Optional accessor for clients that want to short-circuit on unknown event types.
     */
    public Optional<Double> findWeight(String eventType) {
        return Optional.ofNullable(weightFor(eventType));
    }

    /**
     * @return canonical event type 집합 (immutable view).
     */
    public Map<String, Double> canonicalWeights() {
        return CANONICAL_WEIGHTS;
    }
}
