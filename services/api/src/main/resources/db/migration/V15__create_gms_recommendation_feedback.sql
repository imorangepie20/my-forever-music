CREATE TABLE gms_recommendation_feedback (
    feedback_id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    request_id VARCHAR(160),
    playlist_id VARCHAR(160),
    track_id VARCHAR(160) NOT NULL,
    feedback_type VARCHAR(30) NOT NULL,
    feedback_score INTEGER,
    source_space VARCHAR(50),
    reason VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_gms_recommendation_feedback_track
        FOREIGN KEY (track_id) REFERENCES pms_user_track (track_id)
);

CREATE INDEX idx_gms_recommendation_feedback_user_created
    ON gms_recommendation_feedback (user_id, created_at DESC, feedback_id DESC);

CREATE INDEX idx_gms_recommendation_feedback_track_created
    ON gms_recommendation_feedback (track_id, created_at DESC, feedback_id DESC);
