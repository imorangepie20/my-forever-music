CREATE TABLE recommendation_audit_log (
    recommendation_audit_log_id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    recommendation_id VARCHAR(160),
    request_id VARCHAR(160),
    event_type VARCHAR(60) NOT NULL,
    source_space VARCHAR(50),
    model_version VARCHAR(160),
    dataset_version VARCHAR(120),
    dataset_fingerprint VARCHAR(160),
    item_count INTEGER,
    sasrec_applied BOOLEAN,
    fallback_reason VARCHAR(500),
    feedback_type VARCHAR(30),
    target_track_id VARCHAR(200),
    target_playlist_id VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_recommendation_audit_user_created
    ON recommendation_audit_log (user_id, created_at DESC, recommendation_audit_log_id DESC);

CREATE INDEX idx_recommendation_audit_recommendation
    ON recommendation_audit_log (recommendation_id)
    WHERE recommendation_id IS NOT NULL;

CREATE INDEX idx_recommendation_audit_request
    ON recommendation_audit_log (request_id)
    WHERE request_id IS NOT NULL;

CREATE INDEX idx_recommendation_audit_event_created
    ON recommendation_audit_log (event_type, created_at DESC);
