CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(30) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    last_login_at DATETIME(6) NULL,
    deleted_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_nickname UNIQUE (nickname)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    slug VARCHAR(50) NOT NULL,
    display_order INT NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_categories_name UNIQUE (name),
    CONSTRAINT uk_categories_slug UNIQUE (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE posts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    visibility_type VARCHAR(20) NOT NULL,
    failure_size VARCHAR(20) NOT NULL,
    emotion_tag VARCHAR(30) NOT NULL,
    advice_preference VARCHAR(20) NOT NULL,
    retry_intention BOOLEAN NOT NULL,
    reaction_count INT NOT NULL DEFAULT 0,
    report_count INT NOT NULL DEFAULT 0,
    hidden BOOLEAN NOT NULL DEFAULT FALSE,
    hidden_at DATETIME(6) NULL,
    deleted_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_posts_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_posts_category FOREIGN KEY (category_id) REFERENCES categories (id),
    INDEX idx_posts_visible_latest (hidden, deleted_at, created_at),
    INDEX idx_posts_category_visible_latest (category_id, hidden, deleted_at, created_at),
    INDEX idx_posts_visible_popular (hidden, deleted_at, reaction_count, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE post_updates (
    id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    content TEXT NOT NULL,
    deleted_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_post_updates_post FOREIGN KEY (post_id) REFERENCES posts (id),
    CONSTRAINT fk_post_updates_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_post_updates_post_created (post_id, deleted_at, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE reactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reaction_type VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_reaction_post_user UNIQUE (post_id, user_id),
    CONSTRAINT fk_reactions_post FOREIGN KEY (post_id) REFERENCES posts (id),
    CONSTRAINT fk_reactions_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE reports (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reporter_user_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    reason_type VARCHAR(20) NOT NULL,
    reason_detail TEXT NULL,
    status VARCHAR(20) NOT NULL,
    processed_at DATETIME(6) NULL,
    processed_by BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_report_reporter_target UNIQUE (reporter_user_id, target_type, target_id),
    CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_user_id) REFERENCES users (id),
    CONSTRAINT fk_reports_processor FOREIGN KEY (processed_by) REFERENCES users (id),
    INDEX idx_reports_status_created (status, created_at),
    INDEX idx_reports_target_status (target_type, target_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE moderation_actions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    admin_user_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    action_type VARCHAR(30) NOT NULL,
    reason TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_moderation_actions_admin FOREIGN KEY (admin_user_id) REFERENCES users (id),
    INDEX idx_moderation_actions_target (target_type, target_id, created_at),
    INDEX idx_moderation_actions_admin (admin_user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
