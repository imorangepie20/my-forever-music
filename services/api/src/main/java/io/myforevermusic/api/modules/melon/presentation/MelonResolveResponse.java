package io.myforevermusic.api.modules.melon.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MelonResolveResponse(
    int rank,
    String melonSongId,
    String melonTitle,
    String melonArtistName,
    String spotifyTrackId,
    String resolvedTitle,
    String resolvedArtistName,
    String resolvedAlbumTitle,
    String imageUrl,
    String spotifyExternalUrl,
    boolean resolved
) {
}
