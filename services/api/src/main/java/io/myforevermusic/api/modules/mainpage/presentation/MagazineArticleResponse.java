package io.myforevermusic.api.modules.mainpage.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MagazineArticleResponse(
    String sourceName,
    String sourceUrl,
    String articleUrl,
    String articleTitle,
    String articleTitleKo,
    String description,
    String descriptionKo,
    String rationale,
    String rationaleKo,
    String imageUrl,
    Instant capturedAt
) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ListEnvelope(List<MagazineArticleResponse> articles) {}
}
