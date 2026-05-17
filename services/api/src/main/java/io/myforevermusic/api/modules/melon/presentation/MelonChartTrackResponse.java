package io.myforevermusic.api.modules.melon.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MelonChartTrackResponse(
    int rank,
    String melonSongId,
    String title,
    String artistName,
    String albumTitle,
    String imageUrl,
    String songExternalUrl,
    Instant snapshotAt
) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ListEnvelope(
        Instant snapshotAt,
        List<MelonChartTrackResponse> tracks
    ) {
    }
}
