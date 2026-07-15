ALTER TABLE posts
    ADD COLUMN next_attempt_plan VARCHAR(500) NULL AFTER retry_intention;
