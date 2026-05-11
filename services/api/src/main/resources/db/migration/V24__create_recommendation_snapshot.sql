CREATE TABLE recommendation_snapshot (
    recommendation_snapshot_id BIGSERIAL PRIMARY KEY,
    recommendation_id VARCHAR(160) NOT NULL,
    request_id VARCHAR(160),
    user_id VARCHAR(100) NOT NULL,
    candidate_track_id VARCHAR(200),
    candidate_playlist_id VARCHAR(200),
    candidate_title VARCHAR(500),
    candidate_artist_name VARCHAR(500),
    source_space VARCHAR(50),
    source_platform VARCHAR(50),
    model_version VARCHAR(80) NOT NULL,
    feature_snapshot_id VARCHAR(160),
    affinity_score DOUBLE PRECISION,
    novelty_score DOUBLE PRECISION,
    coherence_score DOUBLE PRECISION,
    diversity_score DOUBLE PRECISION,
    redundancy_penalty DOUBLE PRECISION,
    confidence_score DOUBLE PRECISION,
    rank INTEGER,
    reason VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_recommendation_snapshot_user_created
    ON recommendation_snapshot (user_id, created_at DESC, recommendation_snapshot_id DESC);

CREATE INDEX idx_recommendation_snapshot_recommendation
    ON recommendation_snapshot (recommendation_id);

CREATE INDEX idx_recommendation_snapshot_request
    ON recommendation_snapshot (request_id)
    WHERE request_id IS NOT NULL;

CREATE INDEX idx_recommendation_snapshot_track
    ON recommendation_snapshot (candidate_track_id)
    WHERE candidate_track_id IS NOT NULL;
