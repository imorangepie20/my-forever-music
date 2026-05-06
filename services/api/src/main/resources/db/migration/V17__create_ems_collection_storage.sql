CREATE TABLE ems_collected_playlist (
    ems_collected_playlist_id BIGSERIAL PRIMARY KEY,
    external_playlist_id VARCHAR(160) NOT NULL,
    title VARCHAR(200) NOT NULL,
    source_platform VARCHAR(50) NOT NULL,
    curator VARCHAR(120) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    cover_image_url VARCHAR(500),
    platform_external_url VARCHAR(500),
    spotify_uri VARCHAR(200),
    track_count INTEGER NOT NULL DEFAULT 0,
    collection_source VARCHAR(50) NOT NULL,
    search_query VARCHAR(200),
    collected_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ,
    CONSTRAINT uk_ems_collected_playlist_external UNIQUE (source_platform, external_playlist_id)
);

CREATE INDEX idx_ems_collected_playlist_collected
    ON ems_collected_playlist (source_platform, collected_at DESC);

CREATE TABLE ems_collected_track (
    ems_collected_track_id BIGSERIAL PRIMARY KEY,
    external_track_id VARCHAR(160) NOT NULL,
    title VARCHAR(200) NOT NULL,
    artist_name VARCHAR(200) NOT NULL,
    source_platform VARCHAR(50) NOT NULL,
    album_title VARCHAR(200),
    album_image_url VARCHAR(500),
    platform_external_url VARCHAR(500),
    spotify_uri VARCHAR(200),
    preview_url VARCHAR(500),
    duration_ms INTEGER,
    collection_source VARCHAR(50) NOT NULL,
    collected_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_ems_collected_track_external UNIQUE (source_platform, external_track_id)
);

CREATE INDEX idx_ems_collected_track_collected
    ON ems_collected_track (source_platform, collected_at DESC);

CREATE TABLE ems_collected_playlist_track (
    ems_collected_playlist_track_id BIGSERIAL PRIMARY KEY,
    ems_collected_playlist_id BIGINT NOT NULL REFERENCES ems_collected_playlist (ems_collected_playlist_id) ON DELETE CASCADE,
    ems_collected_track_id BIGINT NOT NULL REFERENCES ems_collected_track (ems_collected_track_id),
    sort_order INTEGER NOT NULL,
    CONSTRAINT uk_ems_collected_playlist_track UNIQUE (ems_collected_playlist_id, ems_collected_track_id)
);
