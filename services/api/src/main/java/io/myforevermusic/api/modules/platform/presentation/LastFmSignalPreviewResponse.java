package io.myforevermusic.api.modules.platform.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record LastFmSignalPreviewResponse(
    String service,
    String status,
    Instant generatedAt,
    PreviewRequest request,
    LastFmUserProfile user,
    PreviewSummary summary,
    List<SignalInsight> insights,
    List<RecentTrack> recentTracks,
    List<TopArtist> topArtists,
    List<TopTrack> topTracks
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PreviewRequest(
        String username,
        String period,
        Integer recentLimit,
        Integer topLimit
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record LastFmUserProfile(
        String username,
        String realName,
        String country,
        Long playcount,
        String profileUrl,
        String avatarUrl,
        Instant registeredAt
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PreviewSummary(
        String source,
        Integer recentTrackCount,
        Integer topArtistCount,
        Integer topTrackCount,
        boolean nowPlaying,
        Integer distinctRecentArtistCount,
        String nextStepMessage
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SignalInsight(
        String insightId,
        String title,
        String detail
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record RecentTrack(
        String trackName,
        String artistName,
        String albumName,
        String trackUrl,
        String imageUrl,
        boolean nowPlaying,
        Instant playedAt,
        boolean loved
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record TopArtist(
        String artistName,
        Integer rank,
        Long playcount,
        String artistUrl,
        String imageUrl
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record TopTrack(
        String trackName,
        String artistName,
        Integer rank,
        Long playcount,
        String trackUrl,
        String artistUrl,
        String imageUrl
    ) {
    }
}
