CREATE TABLE pms_playlist (
    playlist_id VARCHAR(100) PRIMARY KEY,
    owner_user_id VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    source_platform VARCHAR(50) NOT NULL,
    track_count INTEGER NOT NULL,
    curator VARCHAR(50) NOT NULL,
    highlight VARCHAR(500) NOT NULL,
    display_order INTEGER NOT NULL
);

CREATE TABLE pms_track (
    track_id VARCHAR(100) PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    artist_name VARCHAR(200) NOT NULL,
    source_platform VARCHAR(50) NOT NULL,
    primary_genre VARCHAR(100)
);

CREATE TABLE pms_playlist_track (
    playlist_track_id BIGSERIAL PRIMARY KEY,
    playlist_id VARCHAR(100) NOT NULL REFERENCES pms_playlist (playlist_id) ON DELETE CASCADE,
    track_id VARCHAR(100) NOT NULL REFERENCES pms_track (track_id) ON DELETE CASCADE,
    sort_order INTEGER NOT NULL,
    is_seed BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_pms_playlist_track UNIQUE (playlist_id, track_id)
);

CREATE INDEX idx_pms_playlist_display_order
    ON pms_playlist (display_order, playlist_id);

CREATE INDEX idx_pms_playlist_track_playlist_order
    ON pms_playlist_track (playlist_id, sort_order, playlist_track_id);
