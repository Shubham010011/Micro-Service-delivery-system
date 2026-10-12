-- Idempotent (IF NOT EXISTS) so databases previously created by Hibernate ddl-auto=update adopt Flyway cleanly.
CREATE TABLE IF NOT EXISTS users (
    id            VARCHAR(255) PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255),
    phone_number  VARCHAR(255),
    role          VARCHAR(255) NOT NULL,
    google_id     VARCHAR(255),
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email ON users (lower(email));
CREATE INDEX IF NOT EXISTS idx_users_google_id ON users (google_id);
