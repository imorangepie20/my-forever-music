CREATE TABLE pms_personal_playlist (
    personal_playlist_id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    playlist_id VARCHAR(160) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_pms_personal_playlist_user_playlist UNIQUE (user_id, playlist_id)
);

CREATE INDEX idx_pms_personal_playlist_user_updated
    ON pms_personal_playlist (user_id, updated_at DESC, playlist_id ASC);

CREATE TABLE pms_personal_playlist_track (
    personal_playlist_track_id BIGSERIAL PRIMARY KEY,
    personal_playlist_id BIGINT NOT NULL REFERENCES pms_personal_playlist (personal_playlist_id) ON DELETE CASCADE,
    track_id VARCHAR(160) NOT NULL REFERENCES pms_user_track (track_id),
    sort_order INTEGER NOT NULL,
    source_context VARCHAR(80) NOT NULL,
    added_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_pms_personal_playlist_track UNIQUE (personal_playlist_id, track_id),
    CONSTRAINT uk_pms_personal_playlist_track_sort UNIQUE (personal_playlist_id, sort_order)
);

CREATE INDEX idx_pms_personal_playlist_track_playlist_sort
    ON pms_personal_playlist_track (personal_playlist_id, sort_order, personal_playlist_track_id);
