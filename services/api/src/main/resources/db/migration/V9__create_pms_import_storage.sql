CREATE TABLE pms_imported_playlist (
    imported_playlist_id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    playlist_id VARCHAR(160) NOT NULL,
    external_playlist_id VARCHAR(160) NOT NULL,
    title VARCHAR(200) NOT NULL,
    source_platform VARCHAR(50) NOT NULL,
    curator VARCHAR(120) NOT NULL,
    highlight VARCHAR(1000) NOT NULL,
    imported_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_pms_imported_playlist_user_playlist UNIQUE (user_id, playlist_id)
);

CREATE INDEX idx_pms_imported_playlist_user_imported_at
    ON pms_imported_playlist (user_id, imported_at DESC, playlist_id ASC);

CREATE TABLE pms_imported_track (
    track_id VARCHAR(160) PRIMARY KEY,
    external_track_id VARCHAR(160) NOT NULL,
    title VARCHAR(200) NOT NULL,
    artist_name VARCHAR(200) NOT NULL,
    source_platform VARCHAR(50) NOT NULL,
    primary_genre VARCHAR(100),
    spotify_track_id VARCHAR(100),
    spotify_audio_feature_source VARCHAR(50) NOT NULL,
    spotify_audio_features_filled BOOLEAN NOT NULL,
    spotify_analysis_url VARCHAR(300),
    spotify_track_href VARCHAR(300),
    spotify_uri VARCHAR(200),
    spotify_feature_type VARCHAR(30) NOT NULL,
    spotify_duration_ms INTEGER,
    spotify_key INTEGER,
    spotify_mode INTEGER,
    spotify_time_signature INTEGER,
    spotify_acousticness DOUBLE PRECISION,
    spotify_danceability DOUBLE PRECISION,
    spotify_energy DOUBLE PRECISION,
    spotify_instrumentalness DOUBLE PRECISION,
    spotify_liveness DOUBLE PRECISION,
    spotify_loudness DOUBLE PRECISION,
    spotify_speechiness DOUBLE PRECISION,
    spotify_tempo DOUBLE PRECISION,
    spotify_valence DOUBLE PRECISION,
    spotify_resolved_at TIMESTAMPTZ
);

CREATE TABLE pms_imported_playlist_track (
    imported_playlist_track_id BIGSERIAL PRIMARY KEY,
    imported_playlist_id BIGINT NOT NULL REFERENCES pms_imported_playlist (imported_playlist_id) ON DELETE CASCADE,
    track_id VARCHAR(160) NOT NULL REFERENCES pms_imported_track (track_id),
    sort_order INTEGER NOT NULL,
    is_seed BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_pms_imported_playlist_track UNIQUE (imported_playlist_id, track_id),
    CONSTRAINT uk_pms_imported_playlist_track_sort UNIQUE (imported_playlist_id, sort_order)
);

CREATE INDEX idx_pms_imported_playlist_track_playlist_sort
    ON pms_imported_playlist_track (imported_playlist_id, sort_order, imported_playlist_track_id);
