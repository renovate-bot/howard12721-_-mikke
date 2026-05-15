CREATE TABLE feed_items (
    id BINARY(16) NOT NULL,
    viewer_user_id BINARY(16) NOT NULL,
    post_id BINARY(16) NOT NULL,
    author_user_id BINARY(16) NOT NULL,
    item_type VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    read_at TIMESTAMP(6) NULL,
    hidden_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_feed_items_viewer_post (viewer_user_id, post_id),
    KEY idx_feed_items_viewer_hidden_created (viewer_user_id, hidden_at, created_at, id),
    KEY idx_feed_items_viewer_read (viewer_user_id, read_at),
    KEY idx_feed_items_post_id (post_id),
    KEY idx_feed_items_author_user_id (author_user_id)
);

CREATE TABLE feed_processed_events (
    event_id BINARY(16) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    processed_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (event_id)
);
