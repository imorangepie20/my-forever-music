package io.myforevermusic.api.modules.recommendation.application;

public record PlaylistQualityEvaluation(
    Double coherenceScore,
    Double diversityScore,
    Double redundancyPenalty
) {
}
