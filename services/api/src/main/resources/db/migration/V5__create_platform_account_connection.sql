CREATE TABLE platform_account_connection (
    connection_id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    platform_id VARCHAR(50) NOT NULL,
    connected BOOLEAN NOT NULL DEFAULT FALSE,
    connection_status VARCHAR(50) NOT NULL,
    connection_mode VARCHAR(50) NOT NULL,
    external_account_label VARCHAR(200),
    scope_summary VARCHAR(200),
    sync_ready BOOLEAN NOT NULL DEFAULT FALSE,
    connected_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_platform_account_connection UNIQUE (user_id, platform_id)
);

CREATE INDEX idx_platform_account_connection_user_id
    ON platform_account_connection (user_id, platform_id);
