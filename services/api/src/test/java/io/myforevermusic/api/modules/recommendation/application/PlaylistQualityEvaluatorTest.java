package io.myforevermusic.api.modules.recommendation.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.myforevermusic.api.modules.gms.presentation.GmsRecommendationPreviewResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlaylistQualityEvaluatorTest {

    private final PlaylistQualityEvaluator evaluator = new PlaylistQualityEvaluator();

    @Test
    void shouldScoreBalancedPlaylistQuality() {
        PlaylistQualityEvaluation evaluation = evaluator.evaluate(List.of(
            item("track-001", "Artist A", "spotify", "playlist-001", 3),
            item("track-002", "Artist B", "spotify", "playlist-001", 4),
            item("track-003", "Artist C", "tidal", "playlist-002", 4)
        ));

        assertThat(evaluation.coherenceScore()).isEqualTo(0.82);
        assertThat(evaluation.diversityScore()).isEqualTo(0.93);
        assertThat(evaluation.redundancyPenalty()).isEqualTo(0.0);
    }

    @Test
    void shouldPenalizeDuplicateTracksAndArtists() {
        PlaylistQualityEvaluation evaluation = evaluator.evaluate(List.of(
            item("track-001", "Artist A", "spotify", "playlist-001", 2),
            item("track-001", "Artist A", "spotify", "playlist-001", 5),
            item("track-002", "Artist A", "spotify", "playlist-001", 1)
        ));

        assertThat(evaluation.coherenceScore()).isEqualTo(0.69);
        assertThat(evaluation.diversityScore()).isEqualTo(0.35);
        assertThat(evaluation.redundancyPenalty()).isEqualTo(0.43);
    }

    private GmsRecommendationPreviewResponse.RecommendationItem item(
        String trackId,
        String artistName,
        String sourcePlatform,
        String sourcePlaylistId,
        Integer energyLevel
    ) {
        return new GmsRecommendationPreviewResponse.RecommendationItem(
            1,
            trackId,
            "Track",
            artistName,
            sourcePlatform,
            sourcePlaylistId,
            "Playlist",
            "Album",
            null,
            null,
            null,
            null,
            trackId,
            180000,
            0.8,
            "gms",
            energyLevel,
            "Reason"
        );
    }
}
