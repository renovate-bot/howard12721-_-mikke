CREATE TABLE friendship_requests (
    id BINARY(16) NOT NULL,
    sender_user_id BINARY(16) NOT NULL,
    receiver_user_id BINARY(16) NOT NULL,
    status VARCHAR(32) NOT NULL,
    pending_pair_key VARBINARY(32)
        GENERATED ALWAYS AS (
            CASE
                WHEN status = 'PENDING' AND sender_user_id < receiver_user_id
                    THEN CONCAT(sender_user_id, receiver_user_id)
                WHEN status = 'PENDING'
                    THEN CONCAT(receiver_user_id, sender_user_id)
                ELSE NULL
            END
        ) STORED,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    responded_at TIMESTAMP(6) NULL,
    canceled_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_friendship_requests_pending_pair (pending_pair_key),
    KEY idx_friendship_requests_sender_status_created (sender_user_id, status, created_at),
    KEY idx_friendship_requests_receiver_status_created (receiver_user_id, status, created_at),
    CONSTRAINT chk_friendship_requests_not_self CHECK (sender_user_id <> receiver_user_id)
);

CREATE TABLE friendships (
    id BINARY(16) NOT NULL,
    user_low_id BINARY(16) NOT NULL,
    user_high_id BINARY(16) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    removed_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_friendships_pair (user_low_id, user_high_id),
    KEY idx_friendships_low_status (user_low_id, status),
    KEY idx_friendships_high_status (user_high_id, status),
    CONSTRAINT chk_friendships_not_self CHECK (user_low_id <> user_high_id)
);

CREATE TABLE blocks (
    blocker_user_id BINARY(16) NOT NULL,
    blocked_user_id BINARY(16) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (blocker_user_id, blocked_user_id),
    KEY idx_blocks_blocked_user_id (blocked_user_id),
    CONSTRAINT chk_blocks_not_self CHECK (blocker_user_id <> blocked_user_id)
);

CREATE TABLE friendship_outbox (
    id BINARY(16) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    event_version INT NOT NULL DEFAULT 1,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id BINARY(16) NOT NULL,
    payload_json JSON NOT NULL,
    correlation_id VARCHAR(128) NULL,
    causation_id BINARY(16) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    published_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    KEY idx_friendship_outbox_unpublished (published_at, created_at)
);
