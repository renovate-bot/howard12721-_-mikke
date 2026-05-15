CREATE TABLE media (
    id BINARY(16) NOT NULL,
    created_by_user_id BINARY(16) NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    content_length_bytes BIGINT NOT NULL,
    etag VARCHAR(255) NULL,
    status VARCHAR(32) NOT NULL,
    upload_method VARCHAR(16) NOT NULL,
    attached_post_id BINARY(16) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    uploaded_at TIMESTAMP(6) NULL,
    attached_at TIMESTAMP(6) NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_media_object_key (object_key),
    KEY idx_media_created_by_created (created_by_user_id, created_at),
    KEY idx_media_attached_post_id (attached_post_id),
    KEY idx_media_status_created (status, created_at),
    CONSTRAINT chk_media_content_length_positive CHECK (content_length_bytes > 0),
    CONSTRAINT chk_media_content_length_max CHECK (content_length_bytes <= 10485760)
);

CREATE TABLE media_variants (
    id BINARY(16) NOT NULL,
    media_id BINARY(16) NOT NULL,
    variant VARCHAR(32) NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    status VARCHAR(32) NOT NULL,
    width INT NULL,
    height INT NULL,
    content_type VARCHAR(128) NOT NULL,
    content_length_bytes BIGINT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ready_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_media_variants_media_variant (media_id, variant),
    UNIQUE KEY uq_media_variants_object_key (object_key),
    KEY idx_media_variants_status_created (status, created_at),
    CONSTRAINT fk_media_variants_media_id
        FOREIGN KEY (media_id) REFERENCES media(id)
        ON DELETE CASCADE
);

CREATE TABLE media_outbox (
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
    KEY idx_media_outbox_unpublished (published_at, created_at)
);
