package io.myforevermusic.api.modules.mainpage.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record HeroTrackListResponse(
    List<HeroTrackResponse> tracks
) {
}
