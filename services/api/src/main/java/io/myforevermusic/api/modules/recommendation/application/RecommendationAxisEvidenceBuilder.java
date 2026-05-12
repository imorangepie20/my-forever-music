package io.myforevermusic.api.modules.recommendation.application;

import java.util.ArrayList;
import java.util.List;

/**
 * 추천 후보 한 곡의 6축(affinity/novelty/coherence/diversity/redundancy/confidence) 점수를
 * 사용자에게 노출할 수 있는 짧은 evidence 텍스트로 변환한다.
 *
 * playlist-level 축(coherence/diversity/redundancy/novelty)은 모든 item에 같은 값이 들어가고,
 * item-level 축(affinity/confidence)은 item별로 다르다.
 */
public final class RecommendationAxisEvidenceBuilder {

    private RecommendationAxisEvidenceBuilder() {
    }

    public static List<AxisEvidence> build(
        Double affinityScore,
        Double noveltyScore,
        PlaylistQualityEvaluation playlistEvaluation,
        Double confidenceScore
    ) {
        List<AxisEvidence> evidence = new ArrayList<>(6);
        evidence.add(affinity(affinityScore));
        evidence.add(novelty(noveltyScore));
        evidence.add(coherence(playlistEvaluation == null ? null : playlistEvaluation.coherenceScore()));
        evidence.add(diversity(playlistEvaluation == null ? null : playlistEvaluation.diversityScore()));
        evidence.add(redundancy(playlistEvaluation == null ? null : playlistEvaluation.redundancyPenalty()));
        evidence.add(confidence(confidenceScore));
        return evidence;
    }

    private static AxisEvidence affinity(Double score) {
        if (score == null) {
            return new AxisEvidence("affinity", null, "unknown", "취향 점수를 계산하지 못했습니다.");
        }
        String level = level(score, 0.7d, 0.5d);
        String summary = switch (level) {
            case "strong" -> "사용자 PMS 라이브러리 신호와 잘 맞는 후보입니다.";
            case "moderate" -> "사용자 취향 신호와 부분적으로 겹치는 후보입니다.";
            default -> "사용자 신호와의 일치도가 낮아 cold-start 보조 후보로 분류됩니다.";
        };
        return new AxisEvidence("affinity", score, level, summary);
    }

    private static AxisEvidence novelty(Double score) {
        if (score == null) {
            return new AxisEvidence("novelty", null, "unknown", "novelty 점수가 없어 기본 베이스라인을 사용합니다.");
        }
        String level = level(score, 0.65d, 0.35d);
        String summary = switch (level) {
            case "strong" -> "최근 청취 패턴과 거리가 있는 새로운 발견입니다.";
            case "moderate" -> "익숙한 톤이면서도 변주가 있는 후보입니다.";
            default -> "최근 자주 듣던 영역에 머무르는 안정적 후보입니다.";
        };
        return new AxisEvidence("novelty", score, level, summary);
    }

    private static AxisEvidence coherence(Double score) {
        if (score == null) {
            return new AxisEvidence("coherence", null, "unknown", "playlist 흐름 점수를 계산하지 못했습니다.");
        }
        String level = level(score, 0.7d, 0.45d);
        String summary = switch (level) {
            case "strong" -> "playlist 안에서 mood/source 흐름이 일관된 후보입니다.";
            case "moderate" -> "흐름이 다소 흩어져 있어 boundary 후보일 수 있습니다.";
            default -> "playlist 전체 흐름과 어긋날 수 있으니 GMS 검토가 필요합니다.";
        };
        return new AxisEvidence("coherence", score, level, summary);
    }

    private static AxisEvidence diversity(Double score) {
        if (score == null) {
            return new AxisEvidence("diversity", null, "unknown", "playlist 다양성 점수를 계산하지 못했습니다.");
        }
        String level = level(score, 0.65d, 0.4d);
        String summary = switch (level) {
            case "strong" -> "artist/genre/platform 분포가 넓어 단조롭지 않습니다.";
            case "moderate" -> "일부 영역에 집중된 분포로 추가 다양성 보강 여지가 있습니다.";
            default -> "playlist가 특정 artist/source에 편중되어 다양성이 낮습니다.";
        };
        return new AxisEvidence("diversity", score, level, summary);
    }

    private static AxisEvidence redundancy(Double penalty) {
        if (penalty == null) {
            return new AxisEvidence("redundancy", null, "unknown", "중복 패널티를 계산하지 못했습니다.");
        }
        String level = redundancyLevel(penalty);
        String summary = switch (level) {
            case "strong" -> "중복 artist/트랙 비율이 높아 정리가 필요합니다.";
            case "moderate" -> "일부 중복이 보이지만 허용 범위입니다.";
            default -> "중복 artist/트랙이 거의 없습니다.";
        };
        return new AxisEvidence("redundancy", penalty, level, summary);
    }

    private static AxisEvidence confidence(Double score) {
        if (score == null) {
            return new AxisEvidence("confidence", null, "unknown", "metadata confidence 점수를 계산하지 못했습니다.");
        }
        String level = level(score, 0.75d, 0.55d);
        String summary = switch (level) {
            case "strong" -> "trackId, audio feature, source playlist 단서가 모두 갖춰진 후보입니다.";
            case "moderate" -> "metadata 일부가 비어 있어 보강 여지가 있습니다.";
            default -> "metadata 신뢰도가 낮아 추가 검증이 필요합니다.";
        };
        return new AxisEvidence("confidence", score, level, summary);
    }

    private static String level(double score, double strongThreshold, double moderateThreshold) {
        if (score >= strongThreshold) {
            return "strong";
        }
        if (score >= moderateThreshold) {
            return "moderate";
        }
        return "low";
    }

    private static String redundancyLevel(double penalty) {
        // penalty는 0(중복 없음) ~ 1(전부 중복). 낮을수록 좋다.
        if (penalty >= 0.5d) {
            return "strong"; // 중복이 강함(부정적 신호 강함)
        }
        if (penalty >= 0.2d) {
            return "moderate";
        }
        return "low";
    }
}
