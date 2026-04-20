のCREATE TABLE identity_users (
    id CHAR(36) NOT NULL,
    email VARCHAR(255) NOT NULL,
    normalized_email VARCHAR(255) NOT NULL,
    username VARCHAR(32) NOT NULL,
    normalized_username VARCHAR(32) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    password_hash_iterations INT NOT NULL,
    password_hash VARCHAR(512) NOT NULL,
    password_salt VARCHAR(512) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_identity_users_normalized_email (normalized_email),
    UNIQUE KEY uq_identity_users_normalized_username (normalized_username)
);
