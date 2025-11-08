CREATE TABLE IF NOT EXISTS notes
(
    id      BIGSERIAL PRIMARY KEY,
    title   VARCHAR(255) NOT NULL,
    content TEXT
);