ALTER TABLE ems_collected_track
    ADD COLUMN audio_feature_track_id VARCHAR(100),
    ADD COLUMN audio_feature_source VARCHAR(50) NOT NULL DEFAULT 'unresolved',
    ADD COLUMN audio_features_filled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN audio_analysis_url VARCHAR(300),
    ADD COLUMN audio_track_href VARCHAR(300),
    ADD COLUMN audio_track_uri VARCHAR(200),
    ADD COLUMN audio_feature_type VARCHAR(30) NOT NULL DEFAULT 'audio_features',
    ADD COLUMN audio_duration_ms INTEGER,
    ADD COLUMN audio_key INTEGER,
    ADD COLUMN audio_mode INTEGER,
    ADD COLUMN audio_time_signature INTEGER,
    ADD COLUMN audio_acousticness DOUBLE PRECISION,
    ADD COLUMN audio_danceability DOUBLE PRECISION,
    ADD COLUMN audio_energy DOUBLE PRECISION,
    ADD COLUMN audio_instrumentalness DOUBLE PRECISION,
    ADD COLUMN audio_liveness DOUBLE PRECISION,
    ADD COLUMN audio_loudness DOUBLE PRECISION,
    ADD COLUMN audio_speechiness DOUBLE PRECISION,
    ADD COLUMN audio_tempo DOUBLE PRECISION,
    ADD COLUMN audio_valence DOUBLE PRECISION,
    ADD COLUMN audio_resolved_at TIMESTAMPTZ;

CREATE INDEX idx_ems_collected_track_audio_coverage
    ON ems_collected_track (source_platform, audio_features_filled);
