CREATE TABLE magazine_article_cache (
    article_url VARCHAR(500) PRIMARY KEY,
    source_name VARCHAR(160) NOT NULL,
    article_title VARCHAR(500) NOT NULL,
    article_title_ko TEXT,
    description TEXT,
    description_ko TEXT,
    rationale TEXT,
    rationale_ko TEXT,
    image_url VARCHAR(1000),
    enriched_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_magazine_article_cache_enriched_at
    ON magazine_article_cache (enriched_at DESC);
