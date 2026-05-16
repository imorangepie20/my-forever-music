package io.myforevermusic.api.modules.pms.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.myforevermusic.api.modules.pms.application.PmsPersonalPlaylistStore;
import java.time.Instant;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PmsPersonalPlaylistBootstrapResponse(
    String service,
    String status,
    Instant generatedAt,
    String userId,
    Summary summary,
    List<Playlist> playlists
) {

    public static PmsPersonalPlaylistBootstrapResponse from(
        String userId,
        List<PmsPersonalPlaylistStore.PersonalPlaylistState> playlists
    ) {
        int trackCount = playlists.stream()
            .mapToInt(PmsPersonalPlaylistStore.PersonalPlaylistState::trackCount)
            .sum();

        return new PmsPersonalPlaylistBootstrapResponse(
            "pms-personal-playlists",
            "ready",
            Instant.now(),
            userId,
            new Summary(playlists.size(), trackCount),
            playlists.stream().map(Playlist::from).toList()
        );
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Summary(
        Integer playlistCount,
        Integer savedTrackCount
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Playlist(
        String playlistId,
        String title,
        String description,
        Integer trackCount,
        Instant createdAt,
        Instant updatedAt,
        List<Track> tracks
    ) {

        public static Playlist from(PmsPersonalPlaylistStore.PersonalPlaylistState state) {
            return new Playlist(
                state.playlistId(),
                state.title(),
                state.description(),
                state.trackCount(),
                state.createdAt(),
                state.updatedAt(),
                state.tracks().stream().map(Track::from).toList()
            );
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Track(
        String trackId,
        String externalTrackId,
        String title,
        String artistName,
        String sourcePlatform,
        String isrc,
        String albumTitle,
        String albumImageUrl,
        String platformExternalUrl,
        String platformUri,
        String previewUrl,
        String spotifyTrackId,
        String spotifyUri,
        String tidalTrackId,
        String tidalUri,
        String preferredPlaybackPlatform,
        String playbackTargetStatus,
        String audioFeatureTrackId,
        Integer durationMs,
        Integer sortOrder,
        String sourceContext,
        Instant addedAt
    ) {
        public Track {
            if (
                (audioFeatureTrackId == null || audioFeatureTrackId.isBlank())
                    && spotifyTrackId != null
                    && !spotifyTrackId.isBlank()
            ) {
                audioFeatureTrackId = spotifyTrackId;
            }
            if (
                (spotifyTrackId == null || spotifyTrackId.isBlank())
                    && audioFeatureTrackId != null
                    && !audioFeatureTrackId.isBlank()
            ) {
                spotifyTrackId = audioFeatureTrackId;
            }
        }

        public static Track from(PmsPersonalPlaylistStore.PersonalTrackState state) {
            return new Track(
                state.trackId(),
                state.externalTrackId(),
                state.title(),
                state.artistName(),
                state.sourcePlatform(),
                state.isrc(),
                state.albumTitle(),
                state.albumImageUrl(),
                state.platformExternalUrl(),
                state.platformUri(),
                state.previewUrl(),
                state.spotifyTrackId(),
                state.spotifyUri(),
                state.tidalTrackId(),
                state.tidalUri(),
                state.preferredPlaybackPlatform(),
                state.playbackTargetStatus(),
                state.audioFeatureTrackId(),
                state.durationMs(),
                state.sortOrder(),
                state.sourceContext(),
                state.addedAt()
            );
        }
    }
}
