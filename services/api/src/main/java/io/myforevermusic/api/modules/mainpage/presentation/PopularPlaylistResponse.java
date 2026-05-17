package io.myforevermusic.api.modules.mainpage.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PopularPlaylistResponse(
    Long playlistId,
    String externalPlaylistId,
    String sourcePlatform,
    String title,
    String curator,
    String description,
    String coverImageUrl,
    String platformExternalUrl,
    int trackCount
) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ListEnvelope(
        List<PopularPlaylistResponse> playlists
    ) {
    }
}
