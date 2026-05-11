package io.myforevermusic.api.modules.recommendation.application;

import io.myforevermusic.api.modules.gms.presentation.GmsRecommendationPreviewResponse;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class PlaylistQualityEvaluator {

    public PlaylistQualityEvaluation evaluate(List<GmsRecommendationPreviewResponse.RecommendationItem> items) {
        if (items == null || items.isEmpty()) {
            return new PlaylistQualityEvaluation(null, null, null);
        }

        int total = items.size();
        double sourcePlaylistCohesion = sourcePlaylistCohesion(items, total);
        double energyCohesion = energyCohesion(items);
        double coherenceScore = clamp((sourcePlaylistCohesion * 0.65d) + (energyCohesion * 0.35d));

        double artistDiversity = uniqueRatio(items, item -> normalize(item.artistName()), total);
        double playlistDiversity = cappedUniqueRatio(items, item -> normalize(item.sourcePlaylistId()), total, 3);
        double platformDiversity = cappedUniqueRatio(items, item -> normalize(item.sourcePlatform()), total, 2);
        double diversityScore = clamp(
            (artistDiversity * 0.70d) + (playlistDiversity * 0.20d) + (platformDiversity * 0.10d)
        );

        double trackRedundancy = 1.0d - uniqueRatio(items, item -> normalize(item.trackId()), total);
        double artistRedundancy = 1.0d - uniqueRatio(items, item -> normalize(item.artistName()), total);
        double redundancyPenalty = clamp((trackRedundancy * 0.70d) + (artistRedundancy * 0.30d));

        return new PlaylistQualityEvaluation(
            round(coherenceScore),
            round(diversityScore),
            round(redundancyPenalty)
        );
    }

    private double sourcePlaylistCohesion(
        List<GmsRecommendationPreviewResponse.RecommendationItem> items,
        int total
    ) {
        long withPlaylist = items.stream()
            .map(item -> normalize(item.sourcePlaylistId()))
            .filter(value -> !value.isBlank())
            .count();
        if (withPlaylist == 0) {
            return 0.55d;
        }

        long dominantCount = items.stream()
            .map(item -> normalize(item.sourcePlaylistId()))
            .filter(value -> !value.isBlank())
            .collect(java.util.stream.Collectors.groupingBy(Function.identity(), java.util.stream.Collectors.counting()))
            .values()
            .stream()
            .mapToLong(Long::longValue)
            .max()
            .orElse(0L);

        double coverage = (double) withPlaylist / total;
        double concentration = (double) dominantCount / withPlaylist;
        return clamp((coverage * 0.35d) + (concentration * 0.65d));
    }

    private double energyCohesion(List<GmsRecommendationPreviewResponse.RecommendationItem> items) {
        List<Integer> energies = items.stream()
            .map(GmsRecommendationPreviewResponse.RecommendationItem::energyLevel)
            .filter(Objects::nonNull)
            .toList();
        if (energies.size() < 2) {
            return 0.70d;
        }

        double averageDelta = 0.0d;
        for (int index = 1; index < energies.size(); index++) {
            averageDelta += Math.abs(energies.get(index) - energies.get(index - 1));
        }
        averageDelta = averageDelta / (energies.size() - 1);
        return clamp(1.0d - (averageDelta / 4.0d));
    }

    private double uniqueRatio(
        List<GmsRecommendationPreviewResponse.RecommendationItem> items,
        Function<GmsRecommendationPreviewResponse.RecommendationItem, String> extractor,
        int total
    ) {
        if (total == 0) {
            return 0.0d;
        }
        long uniqueCount = items.stream()
            .map(extractor)
            .filter(value -> !value.isBlank())
            .distinct()
            .count();
        return clamp((double) uniqueCount / total);
    }

    private double cappedUniqueRatio(
        List<GmsRecommendationPreviewResponse.RecommendationItem> items,
        Function<GmsRecommendationPreviewResponse.RecommendationItem, String> extractor,
        int total,
        int cap
    ) {
        if (total == 0) {
            return 0.0d;
        }
        long uniqueCount = items.stream()
            .map(extractor)
            .filter(value -> !value.isBlank())
            .distinct()
            .count();
        return clamp((double) uniqueCount / Math.min(total, cap));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private Double round(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }

    private double clamp(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0d;
        }
        return Math.min(1.0d, Math.max(0.0d, value));
    }
}
