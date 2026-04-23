CREATE TABLE identity_refresh_sessions (
    id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    refresh_token_hash CHAR(64) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    revoked_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_identity_refresh_sessions_refresh_token_hash (refresh_token_hash),
    KEY idx_identity_refresh_sessions_user_id (user_id),
    CONSTRAINT fk_identity_refresh_sessions_user_id
        FOREIGN KEY (user_id) REFERENCES identity_users(id)
        ON DELETE CASCADE
);
