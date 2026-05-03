CREATE TABLE platform_authorization_session (
    session_id BIGSERIAL PRIMARY KEY,
    state VARCHAR(120) NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    platform_id VARCHAR(50) NOT NULL,
    platform_display_name VARCHAR(120) NOT NULL,
    authorization_mode VARCHAR(50) NOT NULL,
    requested_scopes VARCHAR(300) NOT NULL,
    status VARCHAR(50) NOT NULL,
    approval_code VARCHAR(80) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    CONSTRAINT uk_platform_authorization_session_state UNIQUE (state)
);

CREATE INDEX idx_platform_authorization_session_user_state
    ON platform_authorization_session (user_id, state);
