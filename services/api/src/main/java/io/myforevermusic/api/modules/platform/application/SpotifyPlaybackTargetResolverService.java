package io.myforevermusic.api.modules.platform.application;

import io.myforevermusic.api.common.error.ApiResourceNotFoundException;
import io.myforevermusic.api.modules.platform.infrastructure.spotify.SpotifyWebApiClient;
import io.myforevermusic.api.modules.platform.infrastructure.spotify.SpotifyWebApiClient.SpotifyPlaylistTrack;
import java.util.Comparator;
import org.springframework.stereotype.Service;

@Service
public class SpotifyPlaybackTargetResolverService {

    private static final String SPOTIFY_PLATFORM_ID = "spotify";
    private static final int SEARCH_LIMIT = 10;
    private static final int DURATION_TOLERANCE_MS = 3_000;

    private final PlatformCredentialService platformCredentialService;
    private final SpotifyWebApiClient spotifyWebApiClient;

    public SpotifyPlaybackTargetResolverService(
        PlatformCredentialService platformCredentialService,
        SpotifyWebApiClient spotifyWebApiClient
    ) {
        this.platformCredentialService = platformCredentialService;
        this.spotifyWebApiClient = spotifyWebApiClient;
    }

    public SpotifyPlaybackTarget resolve(String userId, TrackQuery query) {
        PlatformCredentialResolution resolution = platformCredentialService.resolveCredential(userId, SPOTIFY_PLATFORM_ID);
        if (PlatformCredentialResolution.STATUS_MISSING.equals(resolution.status())) {
            throw new IllegalArgumentException("No stored Spotify credential exists for playback target resolution.");
        }
        if (!resolution.usable()) {
            throw new PlatformReconnectRequiredException(
                SPOTIFY_PLATFORM_ID,
                resolution.detail() == null || resolution.detail().isBlank()
                    ? "Reconnect Spotify before resolving playback targets."
                    : resolution.detail()
            );
        }

        return spotifyWebApiClient.searchTracks(
                resolution.credential(),
                searchQuery(query),
                SEARCH_LIMIT
            ).items().stream()
            .map(candidate -> scoredMatch(query, candidate))
            .filter(match -> match.score() >= 70)
            .max(Comparator
                .comparingInt(ScoredMatch::score)
                .thenComparingInt(match -> "isrc".equals(match.reason()) ? 1 : 0))
            .map(match -> toTarget(match.candidate(), match.reason(), match.score()))
            .orElseThrow(() -> new ApiResourceNotFoundException(
                "No playable Spotify match was found for \"%s\" by \"%s\"."
                    .formatted(query.title(), query.artistName())
            ));
    }

    private SpotifyPlaybackTarget toTarget(SpotifyPlaylistTrack candidate, String matchReason, int score) {
        return new SpotifyPlaybackTarget(
            candidate.spotifyTrackId(),
            candidate.spotifyUri(),
            candidate.title(),
            candidate.artistName(),
            candidate.albumTitle(),
            candidate.albumImageUrl(),
            candidate.externalUrl(),
            candidate.previewUrl(),
            candidate.isrc(),
            candidate.durationMs(),
            matchReason,
            score
        );
    }

    private ScoredMatch scoredMatch(TrackQuery query, SpotifyPlaylistTrack candidate) {
        if (sameIsrc(query.isrc(), candidate.isrc())) {
            return new ScoredMatch(candidate, 100, "isrc");
        }

        int score = 0;
        if (sameTitle(query.title(), candidate.title())) {
            score += 45;
        }
        if (sameArtist(query.artistName(), candidate.artistName())) {
            score += 35;
        }
        if (closeDuration(query.durationMs(), candidate.durationMs())) {
            score += 20;
        }
        return new ScoredMatch(candidate, score, "metadata");
    }

    private String searchQuery(TrackQuery query) {
        return "%s %s".formatted(query.title(), query.artistName()).trim();
    }

    private boolean sameIsrc(String left, String right) {
        return hasText(left) && hasText(right) && left.trim().equalsIgnoreCase(right.trim());
    }

    private boolean sameTitle(String left, String right) {
        return normalize(left).equals(normalize(right));
    }

    private boolean sameArtist(String left, String right) {
        String normalizedLeft = normalize(left);
        String normalizedRight = normalize(right);
        return !normalizedLeft.isBlank()
            && !normalizedRight.isBlank()
            && (normalizedLeft.equals(normalizedRight)
                || normalizedLeft.contains(normalizedRight)
                || normalizedRight.contains(normalizedLeft));
    }

    private boolean closeDuration(Integer left, Integer right) {
        if (left == null || right == null) {
            return false;
        }
        return Math.abs(left - right) <= DURATION_TOLERANCE_MS;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase()
            .replaceAll("[^a-z0-9가-힣]+", " ")
            .trim()
            .replaceAll("\\s+", " ");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record TrackQuery(
        String title,
        String artistName,
        String sourcePlatform,
        String externalTrackId,
        String platformUri,
        String tidalTrackId,
        String isrc,
        Integer durationMs
    ) {
    }

    public record SpotifyPlaybackTarget(
        String spotifyTrackId,
        String spotifyUri,
        String title,
        String artistName,
        String albumTitle,
        String albumImageUrl,
        String platformExternalUrl,
        String previewUrl,
        String isrc,
        Integer durationMs,
        String matchReason,
        int matchScore
    ) {
    }

    private record ScoredMatch(SpotifyPlaylistTrack candidate, int score, String reason) {
    }
}
