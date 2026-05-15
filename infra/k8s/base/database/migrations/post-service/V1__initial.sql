CREATE TABLE posts (
    id BINARY(16) NOT NULL,
    author_user_id BINARY(16) NOT NULL,
    media_id BINARY(16) NOT NULL,
    caption VARCHAR(500) NOT NULL,
    visibility VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    latitude DECIMAL(9,6) NOT NULL,
    longitude DECIMAL(9,6) NOT NULL,
    accuracy_meters DOUBLE NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_posts_media_id (media_id),
    KEY idx_posts_author_status_created (author_user_id, status, created_at, id),
    KEY idx_posts_status_created (status, created_at, id),
    CONSTRAINT chk_posts_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_posts_longitude CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT chk_posts_accuracy_positive CHECK (accuracy_meters > 0)
);

CREATE TABLE post_outbox (
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
    KEY idx_post_outbox_unpublished (published_at, created_at)
);
