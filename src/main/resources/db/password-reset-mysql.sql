-- Additive migration for an existing production OneShop database. No user/order data is modified.
CREATE TABLE IF NOT EXISTS password_reset_accounts (
    user_id BIGINT NOT NULL,
    version BIGINT NOT NULL,
    token_hash VARCHAR(64),
    expires_at DATETIME(6),
    requested_at DATETIME(6),
    credential_version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id),
    UNIQUE KEY password_reset_token_hash (token_hash),
    CONSTRAINT password_reset_account_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;
