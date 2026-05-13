package io.myforevermusic.api.modules.recommendation.application;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * MusicBrainz/Wikidata/Discogs candidate를 일관되게 0..1 normalized quality score로 평가한다.
 *
 * Why: external identity source마다 raw score 단위와 의미가 달라 auto-accept threshold(`candidate_score >= 0.95`)가
 * source별로 다르게 동작했다. 모든 source가 같은 0..1 스케일을 공유하면 운영자가 한 threshold로 일관되게 다룰 수 있다.
 */
@Component
public class CandidateQualityScorer {

    private static final Pattern WIKIDATA_ARTIST_PATTERN = Pattern.compile(
        "(?:song|single|track|album|recording)\\s+by\\s+(.+?)(?:[,.;()]|$)",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * @param rawSourceScore source의 0..1 normalized score (없으면 null)
     * @return 0..1 quality score (clamp)
     */
    public double scoreFor(
        String queryTitle,
        String queryArtist,
        String candidateTitle,
        String candidateArtist,
        Double rawSourceScore
    ) {
        double titleSim = similarity(queryTitle, candidateTitle);
        boolean hasArtistQuery = queryArtist != null && !queryArtist.isBlank();
        boolean hasArtistCandidate = candidateArtist != null && !candidateArtist.isBlank();

        double base;
        if (hasArtistQuery && hasArtistCandidate) {
            double artistSim = similarity(queryArtist, candidateArtist);
            base = 0.6d * titleSim + 0.4d * artistSim;
        } else if (hasArtistQuery && !hasArtistCandidate) {
            // 사용자는 artist를 줬는데 후보에 없으면 약한 패널티만 (-0.1)
            base = Math.max(0.0d, titleSim - 0.1d);
        } else {
            base = titleSim;
        }

        if (rawSourceScore == null) {
            return clamp(base);
        }
        // 0.2 weight로 raw source score를 blend (MusicBrainz Lucene score처럼 이미 강한 신호일 때 보강)
        return clamp(0.8d * base + 0.2d * rawSourceScore);
    }

    /**
     * Discogs 응답 title은 보통 "Artist - Title" 포맷. " - "로 split해서 artist/title 분리.
     */
    public DiscogsTitleParts parseDiscogsTitle(String discogsTitle) {
        if (discogsTitle == null || discogsTitle.isBlank()) {
            return new DiscogsTitleParts(null, null);
        }
        int separator = discogsTitle.indexOf(" - ");
        if (separator < 0) {
            return new DiscogsTitleParts(null, discogsTitle.trim());
        }
        String artist = discogsTitle.substring(0, separator).trim();
        String title = discogsTitle.substring(separator + 3).trim();
        return new DiscogsTitleParts(
            artist.isBlank() ? null : artist,
            title.isBlank() ? null : title
        );
    }

    /**
     * Wikidata description에서 "song by X"/"single by Y" 같은 패턴이 있으면 artist를 추출한다.
     */
    public String extractWikidataArtist(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        Matcher matcher = WIKIDATA_ARTIST_PATTERN.matcher(description);
        if (!matcher.find()) {
            return null;
        }
        String captured = matcher.group(1);
        return captured == null || captured.isBlank() ? null : captured.trim();
    }

    private double similarity(String left, String right) {
        if (left == null || right == null) {
            return 0.0d;
        }
        Set<String> leftTokens = tokenize(left);
        Set<String> rightTokens = tokenize(right);
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return 0.0d;
        }
        Set<String> intersection = new HashSet<>(leftTokens);
        intersection.retainAll(rightTokens);
        Set<String> union = new HashSet<>(leftTokens);
        union.addAll(rightTokens);
        double jaccard = (double) intersection.size() / union.size();

        // 한 쪽이 다른 쪽 normalized 문자열을 통째로 포함하면 부분 매치 보너스
        String leftNorm = normalize(left);
        String rightNorm = normalize(right);
        if (!leftNorm.isBlank() && !rightNorm.isBlank()
            && (leftNorm.contains(rightNorm) || rightNorm.contains(leftNorm))) {
            jaccard = Math.min(1.0d, jaccard + 0.2d);
        }
        return jaccard;
    }

    private Set<String> tokenize(String value) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            return Set.of();
        }
        return new HashSet<>(Arrays.asList(normalized.split("\\s+")));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^\\p{L}\\p{N}\\s]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private double clamp(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0d;
        }
        return Math.min(1.0d, Math.max(0.0d, value));
    }

    public record DiscogsTitleParts(String artist, String title) {}
}
