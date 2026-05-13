package io.myforevermusic.api.modules.ems.application;

import java.time.Instant;

public record EmsEditorialArticle(
    String sourceName,
    String sourceUrl,
    String articleUrl,
    String title,
    String summary,
    Instant publishedAt
) {
}
