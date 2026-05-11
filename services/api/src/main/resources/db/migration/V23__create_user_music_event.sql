CREATE TABLE user_music_event (
    user_music_event_id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_weight DOUBLE PRECISION,
    source_space VARCHAR(50) NOT NULL,
    source_platform VARCHAR(50),
    playback_platform_id VARCHAR(50),
    item_id VARCHAR(200),
    item_kind VARCHAR(30),
    track_id VARCHAR(200),
    playlist_id VARCHAR(200),
    external_track_id VARCHAR(200),
    platform_uri VARCHAR(500),
    title VARCHAR(500),
    artist_name VARCHAR(500),
    album_title VARCHAR(500),
    isrc VARCHAR(50),
    duration_ms INTEGER,
    position_ms INTEGER,
    play_ratio DOUBLE PRECISION,
    recommendation_id VARCHAR(160),
    metadata_confidence DOUBLE PRECISION,
    occurred_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_user_music_event_user_occurred
    ON user_music_event (user_id, occurred_at DESC, user_music_event_id DESC);

CREATE INDEX idx_user_music_event_type_occurred
    ON user_music_event (event_type, occurred_at DESC);

CREATE INDEX idx_user_music_event_track_occurred
    ON user_music_event (track_id, occurred_at DESC)
    WHERE track_id IS NOT NULL;

CREATE INDEX idx_user_music_event_recommendation
    ON user_music_event (recommendation_id)
    WHERE recommendation_id IS NOT NULL;
