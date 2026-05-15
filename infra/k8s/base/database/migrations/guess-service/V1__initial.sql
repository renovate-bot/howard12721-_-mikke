CREATE TABLE guesses (
    id BINARY(16) NOT NULL,
    post_id BINARY(16) NOT NULL,
    post_author_user_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    guessed_latitude DECIMAL(9,6) NOT NULL,
    guessed_longitude DECIMAL(9,6) NOT NULL,
    correct_latitude DECIMAL(9,6) NOT NULL,
    correct_longitude DECIMAL(9,6) NOT NULL,
    distance_meters DOUBLE NOT NULL,
    score INT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_guesses_post_user (post_id, user_id),
    KEY idx_guesses_user_created (user_id, created_at),
    KEY idx_guesses_post_created (post_id, created_at),
    KEY idx_guesses_post_author_created (post_author_user_id, created_at),
    KEY idx_guesses_score (score),
    CONSTRAINT chk_guesses_score_range CHECK (score BETWEEN 0 AND 1000),
    CONSTRAINT chk_guesses_guessed_latitude CHECK (guessed_latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_guesses_guessed_longitude CHECK (guessed_longitude BETWEEN -180 AND 180),
    CONSTRAINT chk_guesses_correct_latitude CHECK (correct_latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_guesses_correct_longitude CHECK (correct_longitude BETWEEN -180 AND 180)
);

CREATE TABLE guess_user_stats (
    user_id BINARY(16) NOT NULL,
    guess_count BIGINT NOT NULL DEFAULT 0,
    total_score BIGINT NOT NULL DEFAULT 0,
    best_distance_meters DOUBLE NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id),
    KEY idx_guess_user_stats_guess_count (guess_count),
    KEY idx_guess_user_stats_total_score (total_score)
);

CREATE TABLE post_author_stats (
    user_id BINARY(16) NOT NULL,
    post_count BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id),
    KEY idx_post_author_stats_post_count (post_count)
);

CREATE TABLE guess_processed_events (
    event_id BINARY(16) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    processed_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (event_id)
);

CREATE TABLE guess_outbox (
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
    KEY idx_guess_outbox_unpublished (published_at, created_at)
);
