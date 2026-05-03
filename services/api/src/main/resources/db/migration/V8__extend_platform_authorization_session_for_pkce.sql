ALTER TABLE platform_authorization_session
    ADD COLUMN authorization_channel VARCHAR(50) NOT NULL DEFAULT 'internal_approval_page',
    ADD COLUMN external_authorization_url VARCHAR(1000),
    ADD COLUMN redirect_uri VARCHAR(500),
    ADD COLUMN pkce_code_verifier VARCHAR(150);
