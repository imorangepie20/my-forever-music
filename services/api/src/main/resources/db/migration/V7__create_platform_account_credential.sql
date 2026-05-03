CREATE TABLE platform_account_credential (
    credential_id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    platform_id VARCHAR(50) NOT NULL,
    authorization_mode VARCHAR(50) NOT NULL,
    external_user_id VARCHAR(150),
    external_account_label VARCHAR(200),
    access_token VARCHAR(500) NOT NULL,
    refresh_token VARCHAR(500),
    token_type VARCHAR(30) NOT NULL,
    scope_summary VARCHAR(300),
    access_token_expires_at TIMESTAMPTZ,
    issued_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_platform_account_credential UNIQUE (user_id, platform_id)
);

CREATE INDEX idx_platform_account_credential_user_id
    ON platform_account_credential (user_id, platform_id);
