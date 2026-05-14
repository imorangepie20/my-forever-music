package io.myforevermusic.api.modules.recommendation.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EventSignalWeightsTest {

    private final EventSignalWeights weights = new EventSignalWeights();

    @Test
    void canonicalEventReturnsItsWeight() {
        assertThat(weights.weightFor("track_saved")).isEqualTo(2.0d);
        assertThat(weights.weightFor("added_to_playlist")).isEqualTo(2.0d);
        assertThat(weights.weightFor("recommendation_liked")).isEqualTo(2.0d);
        assertThat(weights.weightFor("replay")).isEqualTo(1.5d);
        assertThat(weights.weightFor("play_completed")).isEqualTo(1.0d);
        assertThat(weights.weightFor("playlist_imported")).isEqualTo(0.3d);
        assertThat(weights.weightFor("ignored_recommendation")).isEqualTo(-0.1d);
        assertThat(weights.weightFor("skip_next")).isEqualTo(-0.25d);
        assertThat(weights.weightFor("stopped_midway")).isEqualTo(-0.25d);
        assertThat(weights.weightFor("recommendation_rejected")).isEqualTo(-2.0d);
        assertThat(weights.weightFor("play_started")).isEqualTo(0.0d);
    }

    @Test
    void aliasResolvesToCanonical() {
        assertThat(weights.weightFor("repeat_played")).isEqualTo(weights.weightFor("replay"));
        assertThat(weights.weightFor("skipped_early")).isEqualTo(weights.weightFor("skip_next"));
        assertThat(weights.weightFor("recommendation_saved")).isEqualTo(weights.weightFor("track_saved"));
        assertThat(weights.canonicalOf("repeat_played")).isEqualTo("replay");
        assertThat(weights.canonicalOf("skipped_early")).isEqualTo("skip_next");
        assertThat(weights.canonicalOf("recommendation_saved")).isEqualTo("track_saved");
    }

    @Test
    void unknownEventReturnsNull() {
        assertThat(weights.weightFor("unknown_event_kind")).isNull();
        assertThat(weights.weightFor(null)).isNull();
        assertThat(weights.weightFor("")).isNull();
        assertThat(weights.weightFor("   ")).isNull();
        assertThat(weights.canonicalOf("unknown_event_kind")).isNull();
        assertThat(weights.canonicalOf(null)).isNull();
        assertThat(weights.findWeight("unknown_event_kind")).isEmpty();
    }

    @Test
    void mixedCaseAndWhitespaceNormalized() {
        assertThat(weights.weightFor("  TRACK_SAVED  ")).isEqualTo(2.0d);
        assertThat(weights.weightFor("Repeat_Played")).isEqualTo(weights.weightFor("replay"));
        assertThat(weights.canonicalOf("  REPLAY ")).isEqualTo("replay");
    }
}
