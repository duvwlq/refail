CREATE INDEX idx_posts_user_deleted_created
    ON posts (user_id, deleted_at, created_at);

CREATE INDEX idx_posts_failure_visible_latest
    ON posts (failure_size, hidden, deleted_at, created_at);

CREATE INDEX idx_post_updates_status_deleted
    ON post_updates (status, deleted_at);
