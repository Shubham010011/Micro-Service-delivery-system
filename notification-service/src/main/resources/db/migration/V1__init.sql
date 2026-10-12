CREATE TABLE IF NOT EXISTS notifications (
    id         VARCHAR(255) PRIMARY KEY,
    user_id    VARCHAR(255)  NOT NULL,
    type       VARCHAR(255)  NOT NULL,
    title      VARCHAR(255)  NOT NULL,
    message    VARCHAR(1000),
    is_read    BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_notif_user ON notifications (user_id, created_at DESC);
