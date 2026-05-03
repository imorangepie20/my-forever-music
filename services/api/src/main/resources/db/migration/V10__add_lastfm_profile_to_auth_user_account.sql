ALTER TABLE auth_user_account
    ADD COLUMN last_fm_username VARCHAR(120),
    ADD COLUMN last_fm_connected_at TIMESTAMPTZ;

CREATE INDEX idx_auth_user_account_last_fm_username
    ON auth_user_account (last_fm_username);
