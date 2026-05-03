CREATE TABLE lastfm_scrobble (
    lastfm_scrobble_id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    last_fm_username VARCHAR(120) NOT NULL,
    track_name VARCHAR(300) NOT NULL,
    artist_name VARCHAR(300) NOT NULL,
    album_name VARCHAR(300),
    track_url VARCHAR(500),
    image_url VARCHAR(500),
    played_at TIMESTAMPTZ NOT NULL,
    loved BOOLEAN NOT NULL DEFAULT FALSE,
    synced_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uk_lastfm_scrobble_identity
    ON lastfm_scrobble (user_id, last_fm_username, played_at, artist_name, track_name);

CREATE INDEX idx_lastfm_scrobble_user_played_at
    ON lastfm_scrobble (user_id, played_at DESC, lastfm_scrobble_id DESC);
