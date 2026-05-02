package io.myforevermusic.api.modules.ems.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record EmsWorkspaceAnalysisRequest(
    String userId,
    String playlistId,
    List<String> seedTrackIds,
    List<String> seedArtistNames,
    List<String> seedGenres
) {

    public EmsWorkspaceAnalysisRequest {
        seedTrackIds = seedTrackIds == null ? List.of() : List.copyOf(seedTrackIds);
        seedArtistNames = seedArtistNames == null ? List.of() : List.copyOf(seedArtistNames);
        seedGenres = seedGenres == null ? List.of() : List.copyOf(seedGenres);
    }
}
