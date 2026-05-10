ALTER TABLE pms_imported_track
    ADD COLUMN isrc VARCHAR(32),
    ADD COLUMN tidal_track_id VARCHAR(100),
    ADD COLUMN tidal_uri VARCHAR(200),
    ADD COLUMN preferred_playback_platform VARCHAR(50),
    ADD COLUMN playback_target_status VARCHAR(50) NOT NULL DEFAULT 'unresolved';

UPDATE pms_imported_track
SET
    spotify_track_id = CASE
        WHEN source_platform = 'spotify' AND (spotify_track_id IS NULL OR spotify_track_id = '') THEN external_track_id
        ELSE spotify_track_id
    END,
    spotify_uri = CASE
        WHEN source_platform = 'spotify' AND (spotify_uri IS NULL OR spotify_uri = '') THEN platform_uri
        ELSE spotify_uri
    END,
    tidal_track_id = CASE
        WHEN source_platform = 'tidal' THEN external_track_id
        ELSE tidal_track_id
    END,
    tidal_uri = CASE
        WHEN source_platform = 'tidal' THEN platform_uri
        ELSE tidal_uri
    END,
    preferred_playback_platform = CASE
        WHEN source_platform IN ('spotify', 'tidal') THEN source_platform
        ELSE preferred_playback_platform
    END,
    playback_target_status = CASE
        WHEN source_platform IN ('spotify', 'tidal') THEN 'native'
        ELSE playback_target_status
    END;

CREATE INDEX idx_pms_imported_track_playback_targets
    ON pms_imported_track (source_platform, preferred_playback_platform, playback_target_status);

ALTER TABLE pms_user_track
    ADD COLUMN isrc VARCHAR(32),
    ADD COLUMN tidal_track_id VARCHAR(100),
    ADD COLUMN tidal_uri VARCHAR(200),
    ADD COLUMN preferred_playback_platform VARCHAR(50),
    ADD COLUMN playback_target_status VARCHAR(50) NOT NULL DEFAULT 'unresolved';

UPDATE pms_user_track
SET
    spotify_track_id = CASE
        WHEN source_platform = 'spotify' AND (spotify_track_id IS NULL OR spotify_track_id = '') THEN external_track_id
        ELSE spotify_track_id
    END,
    spotify_uri = CASE
        WHEN source_platform = 'spotify' AND (spotify_uri IS NULL OR spotify_uri = '') THEN platform_uri
        ELSE spotify_uri
    END,
    tidal_track_id = CASE
        WHEN source_platform = 'tidal' THEN external_track_id
        ELSE tidal_track_id
    END,
    tidal_uri = CASE
        WHEN source_platform = 'tidal' THEN platform_uri
        ELSE tidal_uri
    END,
    preferred_playback_platform = CASE
        WHEN source_platform IN ('spotify', 'tidal') THEN source_platform
        ELSE preferred_playback_platform
    END,
    playback_target_status = CASE
        WHEN source_platform IN ('spotify', 'tidal') THEN 'native'
        ELSE playback_target_status
    END;

CREATE INDEX idx_pms_user_track_playback_targets
    ON pms_user_track (source_platform, preferred_playback_platform, playback_target_status);
