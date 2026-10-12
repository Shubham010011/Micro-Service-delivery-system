CREATE TABLE IF NOT EXISTS delivery_partner_profiles (
    partner_id       VARCHAR(255) PRIMARY KEY,
    status           VARCHAR(255) NOT NULL,
    current_order_id VARCHAR(255),
    last_lat         DOUBLE PRECISION,
    last_lng         DOUBLE PRECISION,
    updated_at       TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS delivery_assignments (
    id                  VARCHAR(255) PRIMARY KEY,
    order_id            VARCHAR(255) NOT NULL,
    delivery_partner_id VARCHAR(255) NOT NULL,
    pickup_address      VARCHAR(255),
    drop_address        VARCHAR(255),
    status              VARCHAR(255) NOT NULL,
    delivery_status     VARCHAR(255),
    offered_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    responded_at        TIMESTAMPTZ,
    version             BIGINT       NOT NULL DEFAULT 0
);
-- databases created before optimistic locking was added
ALTER TABLE delivery_assignments ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
CREATE INDEX IF NOT EXISTS idx_assign_order   ON delivery_assignments (order_id, offered_at DESC);
CREATE INDEX IF NOT EXISTS idx_assign_partner ON delivery_assignments (delivery_partner_id, status);

CREATE TABLE IF NOT EXISTS chat_messages (
    id          VARCHAR(255) PRIMARY KEY,
    order_id    VARCHAR(255)  NOT NULL,
    sender_id   VARCHAR(255)  NOT NULL,
    sender_role VARCHAR(255)  NOT NULL,
    message     VARCHAR(1000) NOT NULL,
    sent_at     TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_chat_order ON chat_messages (order_id, sent_at);


CREATE TABLE IF NOT EXISTS pending_dispatches (
    order_id        VARCHAR(255) PRIMARY KEY,
    pickup_address  VARCHAR(255),
    drop_address    VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_attempt_at TIMESTAMPTZ
);
