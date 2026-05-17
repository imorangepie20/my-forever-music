package io.myforevermusic.api.modules.platform.application;

import io.myforevermusic.api.common.error.ApiResourceNotFoundException;
import io.myforevermusic.api.modules.platform.infrastructure.youtube.YouTubeDataApiClient;
import io.myforevermusic.api.modules.platform.infrastructure.youtube.YouTubeDataApiClient.YouTubeVideoCandidate;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class YouTubePlaybackTargetResolverService {

    private static final int SEARCH_LIMIT = 12;
    private static final int DURATION_TOLERANCE_MS = 8_000;

    private final YouTubeDataApiClient youTubeDataApiClient;

    public YouTubePlaybackTargetResolverService(YouTubeDataApiClient youTubeDataApiClient) {
        this.youTubeDataApiClient = youTubeDataApiClient;
    }

    public YouTubePlaybackTarget resolve(TrackQuery query) {
        List<YouTubeVideoCandidate> candidates = youTubeDataApiClient.searchEmbeddableVideos(searchQuery(query), SEARCH_LIMIT);
        return candidates.stream()
            .filter(candidate -> !query.excludedVideoIds().contains(candidate.videoId()))
            .map(candidate -> scoredMatch(query, candidate))
            .filter(match -> match.score() >= 45)
            .max(Comparator
                .comparingInt(ScoredMatch::score)
                .thenComparingInt(match -> closeDuration(query.durationMs(), match.candidate().durationMs()) ? 1 : 0))
            .map(match -> toTarget(match.candidate(), match.score(), candidates.size()))
            .orElseThrow(() -> new ApiResourceNotFoundException(
                "No embeddable YouTube video was found for \"%s\" by \"%s\"."
                    .formatted(query.title(), query.artistName())
            ));
    }

    private YouTubePlaybackTarget toTarget(YouTubeVideoCandidate candidate, int score, int candidateCount) {
        return new YouTubePlaybackTarget(
            candidate.videoId(),
            "https://www.youtube.com/watch?v=%s".formatted(candidate.videoId()),
            candidate.title(),
            candidate.channelTitle(),
            candidate.thumbnailUrl(),
            candidate.durationMs(),
            "youtube_data_api_search",
            score,
            candidateCount
        );
    }

    private ScoredMatch scoredMatch(TrackQuery query, YouTubeVideoCandidate candidate) {
        String haystack = normalize("%s %s %s".formatted(
            candidate.title(),
            candidate.channelTitle(),
            candidate.description()
        ));
        String title = normalize(query.title());
        String artist = normalize(query.artistName());

        int score = 0;
        if (!title.isBlank() && (haystack.contains(title) || title.contains(normalize(candidate.title())))) {
            score += 35;
        }
        if (!artist.isBlank() && haystack.contains(artist)) {
            score += 30;
        }
        if (closeDuration(query.durationMs(), candidate.durationMs())) {
            score += 20;
        }
        if (looksOfficial(candidate, artist)) {
            score += 15;
        }
        if (containsBadMatchTerm(haystack)) {
            score -= 45;
        }
        return new ScoredMatch(candidate, score);
    }

    private String searchQuery(TrackQuery query) {
        String base = "%s %s".formatted(query.title(), query.artistName()).trim();
        return base.isBlank() ? query.title() : base;
    }

    private boolean closeDuration(Integer left, Integer right) {
        if (left == null || right == null || left <= 0 || right <= 0) {
            return false;
        }
        return Math.abs(left - right) <= DURATION_TOLERANCE_MS;
    }

    private boolean looksOfficial(YouTubeVideoCandidate candidate, String normalizedArtist) {
        String title = normalize(candidate.title());
        String channel = normalize(candidate.channelTitle());
        return title.contains("official")
            || title.contains("provided to youtube")
            || channel.contains("official")
            || (!normalizedArtist.isBlank() && channel.contains(normalizedArtist));
    }

    private boolean containsBadMatchTerm(String normalizedValue) {
        return List.of(
            "karaoke",
            "노래방",
            "tj노래방",
            "금영",
            "instrumental",
            "reaction",
            "cover",
            "tutorial",
            "piano version"
        ).stream().anyMatch(normalizedValue::contains);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase()
            .replaceAll("\\([^)]*\\)", " ")
            .replaceAll("\\[[^]]*]", " ")
            .replaceAll("[^a-z0-9가-힣]+", " ")
            .trim()
            .replaceAll("\\s+", " ");
    }

    public record TrackQuery(
        String title,
        String artistName,
        String sourcePlatform,
        String externalTrackId,
        String platformUri,
        String spotifyTrackId,
        String tidalTrackId,
        String isrc,
        Integer durationMs,
        List<String> excludedVideoIds
    ) {
        public TrackQuery {
            title = title == null ? "" : title.trim();
            artistName = artistName == null ? "" : artistName.trim();
            excludedVideoIds = excludedVideoIds == null ? List.of() : excludedVideoIds.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        }
    }

    public record YouTubePlaybackTarget(
        String youtubeVideoId,
        String youtubeUrl,
        String title,
        String channelTitle,
        String thumbnailUrl,
        Integer durationMs,
        String matchReason,
        int matchScore,
        int candidateCount
    ) {
    }

    private record ScoredMatch(YouTubeVideoCandidate candidate, int score) {
    }
}
