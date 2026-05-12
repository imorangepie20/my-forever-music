package io.myforevermusic.api.modules.recommendation.application;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * 단일 6축 점수에 대한 사용자 노출용 evidence.
 * level 은 "strong", "moderate", "low" 셋 중 하나로 짧은 신호 강도,
 * summary 는 한두 문장 한국어 텍스트.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AxisEvidence(
    String axis,
    Double score,
    String level,
    String summary
) {}
