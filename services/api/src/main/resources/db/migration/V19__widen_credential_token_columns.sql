ALTER TABLE platform_account_credential
    ALTER COLUMN access_token TYPE VARCHAR(4000),
    ALTER COLUMN refresh_token TYPE VARCHAR(4000);
