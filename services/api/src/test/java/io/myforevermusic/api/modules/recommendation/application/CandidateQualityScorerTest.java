package io.myforevermusic.api.modules.recommendation.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CandidateQualityScorerTest {

    private final CandidateQualityScorer scorer = new CandidateQualityScorer();

    @Test
    void exactTitleAndArtistMatchScoresHigh() {
        double score = scorer.scoreFor(
            "Bohemian Rhapsody",
            "Queen",
            "Bohemian Rhapsody",
            "Queen",
            null
        );
        assertThat(score).isGreaterThanOrEqualTo(0.95d);
    }

    @Test
    void unrelatedTitleScoresLow() {
        double score = scorer.scoreFor(
            "Bohemian Rhapsody",
            "Queen",
            "Random Other Song",
            "Different Band",
            null
        );
        assertThat(score).isLessThan(0.3d);
    }

    @Test
    void partialTitleMatchAndArtistMatchScoresMid() {
        double score = scorer.scoreFor(
            "Heat Waves",
            "Glass Animals",
            "Heat Waves - Remastered",
            "Glass Animals",
            null
        );
        assertThat(score).isBetween(0.6d, 1.0d);
    }

    @Test
    void rawSourceScoreBlendsIntoComposite() {
        double withoutRaw = scorer.scoreFor(
            "Bohemian Rhapsody",
            "Queen",
            "Bohemian Rhapsody",
            "Queen",
            null
        );
        double withLowRaw = scorer.scoreFor(
            "Bohemian Rhapsody",
            "Queen",
            "Bohemian Rhapsody",
            "Queen",
            0.5d
        );
        assertThat(withLowRaw).isLessThan(withoutRaw);
    }

    @Test
    void missingCandidateArtistAppliesSmallPenaltyWhenQueryHasArtist() {
        double withArtist = scorer.scoreFor("Title", "Artist", "Title", "Artist", null);
        double withoutCandidateArtist = scorer.scoreFor("Title", "Artist", "Title", null, null);
        assertThat(withoutCandidateArtist).isLessThan(withArtist);
        assertThat(withoutCandidateArtist).isGreaterThan(0.0d);
    }

    @Test
    void discogsTitleSplitsOnDash() {
        CandidateQualityScorer.DiscogsTitleParts parts = scorer.parseDiscogsTitle("Queen - Bohemian Rhapsody");
        assertThat(parts.artist()).isEqualTo("Queen");
        assertThat(parts.title()).isEqualTo("Bohemian Rhapsody");
    }

    @Test
    void discogsTitleWithoutDashKeepsTitleOnly() {
        CandidateQualityScorer.DiscogsTitleParts parts = scorer.parseDiscogsTitle("Just A Title");
        assertThat(parts.artist()).isNull();
        assertThat(parts.title()).isEqualTo("Just A Title");
    }

    @Test
    void wikidataArtistExtractedFromSongByPattern() {
        String extracted = scorer.extractWikidataArtist("song by Queen, 1975");
        assertThat(extracted).isEqualTo("Queen");
    }

    @Test
    void wikidataArtistExtractedFromAlbumByPattern() {
        String extracted = scorer.extractWikidataArtist("1975 album by Pink Floyd");
        assertThat(extracted).isEqualTo("Pink Floyd");
    }

    @Test
    void wikidataArtistReturnsNullWhenNoPatternMatches() {
        assertThat(scorer.extractWikidataArtist("a Norwegian municipality")).isNull();
        assertThat(scorer.extractWikidataArtist(null)).isNull();
        assertThat(scorer.extractWikidataArtist("")).isNull();
    }
}
