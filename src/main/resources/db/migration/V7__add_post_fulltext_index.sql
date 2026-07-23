CREATE FULLTEXT INDEX idx_posts_fulltext_search
    ON posts (title, content, emotion_tag)
    WITH PARSER ngram;
