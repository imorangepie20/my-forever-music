package io.myforevermusic.api.modules.mainpage.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "magazine_article_cache")
public class MagazineArticleCacheEntity {

    @Id
    @Column(name = "article_url", nullable = false, length = 500)
    private String articleUrl;

    @Column(name = "source_name", nullable = false, length = 160)
    private String sourceName;

    @Column(name = "article_title", nullable = false, length = 500)
    private String articleTitle;

    @Column(name = "article_title_ko", columnDefinition = "text")
    private String articleTitleKo;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "description_ko", columnDefinition = "text")
    private String descriptionKo;

    @Column(name = "rationale", columnDefinition = "text")
    private String rationale;

    @Column(name = "rationale_ko", columnDefinition = "text")
    private String rationaleKo;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "enriched_at", nullable = false)
    private Instant enrichedAt;

    protected MagazineArticleCacheEntity() {}

    public MagazineArticleCacheEntity(
        String articleUrl,
        String sourceName,
        String articleTitle,
        String articleTitleKo,
        String description,
        String descriptionKo,
        String rationale,
        String rationaleKo,
        String imageUrl,
        Instant enrichedAt
    ) {
        this.articleUrl = articleUrl;
        this.sourceName = sourceName;
        this.articleTitle = articleTitle;
        this.articleTitleKo = articleTitleKo;
        this.description = description;
        this.descriptionKo = descriptionKo;
        this.rationale = rationale;
        this.rationaleKo = rationaleKo;
        this.imageUrl = imageUrl;
        this.enrichedAt = enrichedAt;
    }

    public String getArticleUrl() { return articleUrl; }
    public String getSourceName() { return sourceName; }
    public String getArticleTitle() { return articleTitle; }
    public String getArticleTitleKo() { return articleTitleKo; }
    public String getDescription() { return description; }
    public String getDescriptionKo() { return descriptionKo; }
    public String getRationale() { return rationale; }
    public String getRationaleKo() { return rationaleKo; }
    public String getImageUrl() { return imageUrl; }
    public Instant getEnrichedAt() { return enrichedAt; }
}
