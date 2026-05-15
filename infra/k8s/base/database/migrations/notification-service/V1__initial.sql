CREATE TABLE notifications (
    id BINARY(16) NOT NULL,
    recipient_user_id BINARY(16) NOT NULL,
    type VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body VARCHAR(1024) NOT NULL,
    related_post_id BINARY(16) NULL,
    related_user_id BINARY(16) NULL,
    dedupe_key VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    read_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_notifications_dedupe_key (dedupe_key),
    KEY idx_notifications_recipient_created (recipient_user_id, created_at),
    KEY idx_notifications_recipient_read (recipient_user_id, read_at)
);

CREATE TABLE push_tokens (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    platform VARCHAR(32) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    token_encrypted VARBINARY(2048) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_seen_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    disabled_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_push_tokens_platform_token_hash (platform, token_hash),
    UNIQUE KEY uq_push_tokens_user_device_platform (user_id, device_id, platform),
    KEY idx_push_tokens_user_enabled (user_id, enabled)
);

CREATE TABLE notification_preferences (
    user_id BINARY(16) NOT NULL,
    post_created_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    friend_request_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    friend_accepted_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    guess_submitted_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id)
);

CREATE TABLE notification_deliveries (
    id BINARY(16) NOT NULL,
    notification_id BINARY(16) NOT NULL,
    push_token_id BINARY(16) NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    provider_message_id VARCHAR(255) NULL,
    last_error VARCHAR(1024) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    sent_at TIMESTAMP(6) NULL,
    failed_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    KEY idx_notification_deliveries_notification_id (notification_id),
    KEY idx_notification_deliveries_push_token_id (push_token_id),
    KEY idx_notification_deliveries_status_created (status, created_at),
    CONSTRAINT fk_notification_deliveries_notification_id
        FOREIGN KEY (notification_id) REFERENCES notifications(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_notification_deliveries_push_token_id
        FOREIGN KEY (push_token_id) REFERENCES push_tokens(id)
        ON DELETE CASCADE
);

CREATE TABLE notification_processed_events (
    event_id BINARY(16) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    processed_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (event_id)
);
