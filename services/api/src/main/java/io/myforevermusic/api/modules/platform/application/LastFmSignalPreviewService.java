package io.myforevermusic.api.modules.platform.application;

import io.myforevermusic.api.modules.platform.infrastructure.lastfm.LastFmWebApiClient;
import io.myforevermusic.api.modules.platform.presentation.LastFmSignalPreviewResponse;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class LastFmSignalPreviewService {

    private static final Set<String> SUPPORTED_PERIODS = Set.of(
        "overall",
        "7day",
        "1month",
        "3month",
        "6month",
        "12month"
    );

    private final LastFmWebApiClient lastFmWebApiClient;

    public LastFmSignalPreviewService(LastFmWebApiClient lastFmWebApiClient) {
        this.lastFmWebApiClient = lastFmWebApiClient;
    }

    public LastFmSignalPreviewResponse getPreview(
        String username,
        String period,
        Integer recentLimit,
        Integer topLimit
    ) {
        String normalizedUsername = normalizeUsername(username);
        String normalizedPeriod = normalizePeriod(period);
        int normalizedRecentLimit = clamp(recentLimit, 1, 20, 8);
        int normalizedTopLimit = clamp(topLimit, 1, 12, 6);

        LastFmWebApiClient.LastFmUserProfile profile = lastFmWebApiClient.getUserProfile(normalizedUsername);
        List<LastFmWebApiClient.LastFmRecentTrack> recentTracks = lastFmWebApiClient.getRecentTracks(
            normalizedUsername,
            normalizedRecentLimit
        );
        List<LastFmWebApiClient.LastFmTopArtist> topArtists = lastFmWebApiClient.getTopArtists(
            normalizedUsername,
            normalizedPeriod,
            normalizedTopLimit
        );
        List<LastFmWebApiClient.LastFmTopTrack> topTracks = lastFmWebApiClient.getTopTracks(
            normalizedUsername,
            normalizedPeriod,
            normalizedTopLimit
        );

        List<LastFmSignalPreviewResponse.SignalInsight> insights = buildInsights(
            profile,
            normalizedPeriod,
            recentTracks,
            topArtists,
            topTracks
        );

        return new LastFmSignalPreviewResponse(
            "api",
            "ok",
            Instant.now(),
            new LastFmSignalPreviewResponse.PreviewRequest(
                normalizedUsername,
                normalizedPeriod,
                normalizedRecentLimit,
                normalizedTopLimit
            ),
            new LastFmSignalPreviewResponse.LastFmUserProfile(
                profile.username(),
                profile.realName(),
                profile.country(),
                profile.playcount(),
                profile.profileUrl(),
                profile.avatarUrl(),
                profile.registeredAt()
            ),
            new LastFmSignalPreviewResponse.PreviewSummary(
                "lastfm-public-api",
                recentTracks.size(),
                topArtists.size(),
                topTracks.size(),
                recentTracks.stream().anyMatch(LastFmWebApiClient.LastFmRecentTrack::nowPlaying),
                countDistinctRecentArtists(recentTracks),
                "Use top artists as EMS affinity seeds or keep Last.fm as a long-term taste signal source."
            ),
            insights,
            recentTracks.stream()
                .map(track -> new LastFmSignalPreviewResponse.RecentTrack(
                    track.trackName(),
                    track.artistName(),
                    track.albumName(),
                    track.trackUrl(),
                    track.imageUrl(),
                    track.nowPlaying(),
                    track.playedAt(),
                    track.loved()
                ))
                .toList(),
            topArtists.stream()
                .map(artist -> new LastFmSignalPreviewResponse.TopArtist(
                    artist.artistName(),
                    artist.rank(),
                    artist.playcount(),
                    artist.artistUrl(),
                    artist.imageUrl()
                ))
                .toList(),
            topTracks.stream()
                .map(track -> new LastFmSignalPreviewResponse.TopTrack(
                    track.trackName(),
                    track.artistName(),
                    track.rank(),
                    track.playcount(),
                    track.trackUrl(),
                    track.artistUrl(),
                    track.imageUrl()
                ))
                .toList()
        );
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Last.fm username is required.");
        }
        return username.trim();
    }

    private String normalizePeriod(String period) {
        String normalized = period == null || period.isBlank() ? "1month" : period.trim();
        if (!SUPPORTED_PERIODS.contains(normalized)) {
            throw new IllegalArgumentException(
                "Last.fm period must be one of: overall, 7day, 1month, 3month, 6month, 12month."
            );
        }
        return normalized;
    }

    private int clamp(Integer value, int min, int max, int fallback) {
        if (value == null) {
            return fallback;
        }
        return Math.max(min, Math.min(max, value));
    }

    private int countDistinctRecentArtists(List<LastFmWebApiClient.LastFmRecentTrack> recentTracks) {
        return recentTracks.stream()
            .map(LastFmWebApiClient.LastFmRecentTrack::artistName)
            .filter(value -> value != null && !value.isBlank())
            .collect(Collectors.toCollection(LinkedHashSet::new))
            .size();
    }

    private List<LastFmSignalPreviewResponse.SignalInsight> buildInsights(
        LastFmWebApiClient.LastFmUserProfile profile,
        String period,
        List<LastFmWebApiClient.LastFmRecentTrack> recentTracks,
        List<LastFmWebApiClient.LastFmTopArtist> topArtists,
        List<LastFmWebApiClient.LastFmTopTrack> topTracks
    ) {
        List<LastFmSignalPreviewResponse.SignalInsight> insights = new java.util.ArrayList<>();

        if (!topArtists.isEmpty()) {
            LastFmWebApiClient.LastFmTopArtist anchorArtist = topArtists.getFirst();
            insights.add(new LastFmSignalPreviewResponse.SignalInsight(
                "artist-anchor",
                "Long-Term Artist Anchor",
                "%s is the strongest %s artist signal right now with %s plays.".formatted(
                    fallback(anchorArtist.artistName(), "Unknown Artist"),
                    period,
                    formatCount(anchorArtist.playcount())
                )
            ));
        }

        if (!recentTracks.isEmpty()) {
            LastFmWebApiClient.LastFmRecentTrack firstTrack = recentTracks.getFirst();
            int distinctArtists = countDistinctRecentArtists(recentTracks);
            insights.add(new LastFmSignalPreviewResponse.SignalInsight(
                "recent-motion",
                firstTrack.nowPlaying() ? "Currently Playing" : "Recent Motion",
                firstTrack.nowPlaying()
                    ? "%s is playing %s right now, and the last %s scrobbles span %s distinct artists.".formatted(
                        profile.username(),
                        fallback(firstTrack.trackName(), "an unknown track"),
                        recentTracks.size(),
                        distinctArtists
                    )
                    : "Recent scrobbles are led by %s by %s, with %s distinct artists in the latest %s listens.".formatted(
                        fallback(firstTrack.trackName(), "an unknown track"),
                        fallback(firstTrack.artistName(), "Unknown Artist"),
                        distinctArtists,
                        recentTracks.size()
                    )
            ));
        }

        if (!topTracks.isEmpty()) {
            LastFmWebApiClient.LastFmTopTrack topTrack = topTracks.getFirst();
            insights.add(new LastFmSignalPreviewResponse.SignalInsight(
                "repeat-familiarity",
                "Repeat Familiarity Bias",
                "%s by %s leads the %s chart with %s plays, which is a useful familiarity seed for EMS/GMS ranking.".formatted(
                    fallback(topTrack.trackName(), "Unknown Track"),
                    fallback(topTrack.artistName(), "Unknown Artist"),
                    period,
                    formatCount(topTrack.playcount())
                )
            ));
        }

        return insights;
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String formatCount(Long value) {
        return value == null ? "unknown" : Long.toString(value);
    }
}
