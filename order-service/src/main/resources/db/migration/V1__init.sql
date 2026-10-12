CREATE TABLE IF NOT EXISTS orders (
    id                          VARCHAR(255) PRIMARY KEY,
    customer_id                 VARCHAR(255) NOT NULL,
    pickup_address              VARCHAR(255) NOT NULL,
    drop_address                VARCHAR(255) NOT NULL,
    item_description            VARCHAR(255),
    amount                      DOUBLE PRECISION,
    status                      VARCHAR(255) NOT NULL,
    assigned_delivery_partner_id VARCHAR(255),
    preferred_partner_requested BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_orders_customer ON orders (customer_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_orders_partner  ON orders (assigned_delivery_partner_id, created_at DESC);
