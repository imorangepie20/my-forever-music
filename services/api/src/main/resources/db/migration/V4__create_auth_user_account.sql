CREATE TABLE auth_user_account (
    user_id VARCHAR(100) PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    normalized_email VARCHAR(320) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    password_hash VARCHAR(120) NOT NULL,
    preferred_platform_id VARCHAR(50) NOT NULL,
    marketing_opt_in BOOLEAN NOT NULL DEFAULT FALSE,
    onboarding_stage VARCHAR(50) NOT NULL,
    registered_at TIMESTAMPTZ NOT NULL,
    accepted_terms_at TIMESTAMPTZ NOT NULL,
    accepted_privacy_policy_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_auth_user_account_email UNIQUE (email),
    CONSTRAINT uk_auth_user_account_normalized_email UNIQUE (normalized_email)
);

CREATE INDEX idx_auth_user_account_registered_at
    ON auth_user_account (registered_at DESC, user_id);
