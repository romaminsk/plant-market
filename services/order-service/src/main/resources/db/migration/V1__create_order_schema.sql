-- Phase 2: order aggregate, items, status history and transactional outbox.
CREATE TABLE orders (
    id              uuid         PRIMARY KEY,
    customer_id     uuid         NOT NULL,
    status          varchar(30)  NOT NULL,
    subtotal_minor  bigint       NOT NULL CHECK (subtotal_minor >= 0),
    shipping_minor  bigint       NOT NULL DEFAULT 0 CHECK (shipping_minor >= 0),
    tax_minor       bigint       NOT NULL DEFAULT 0 CHECK (tax_minor >= 0),
    discount_minor  bigint       NOT NULL DEFAULT 0 CHECK (discount_minor >= 0),
    total_minor     bigint       NOT NULL CHECK (total_minor >= 0),
    currency        varchar(3)   NOT NULL,
    reservation_expires_at timestamptz,
    created_at      timestamptz  NOT NULL,
    updated_at      timestamptz  NOT NULL,
    version         bigint       NOT NULL DEFAULT 0
);

CREATE INDEX ix_orders_customer ON orders (customer_id, created_at DESC);
CREATE INDEX ix_orders_status ON orders (status);

CREATE TABLE order_items (
    id               uuid        PRIMARY KEY,
    order_id         uuid        NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    item_type        varchar(20) NOT NULL,
    target_id        uuid        NOT NULL,
    name             varchar(200) NOT NULL,
    unit_price_minor bigint      NOT NULL CHECK (unit_price_minor >= 0),
    quantity         integer     NOT NULL CHECK (quantity > 0),
    line_total_minor bigint      NOT NULL CHECK (line_total_minor >= 0)
);

CREATE INDEX ix_order_items_order ON order_items (order_id);

CREATE TABLE order_status_history (
    id         uuid        PRIMARY KEY,
    order_id   uuid        NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    status     varchar(30) NOT NULL,
    created_at timestamptz NOT NULL
);

CREATE INDEX ix_order_status_history_order ON order_status_history (order_id, created_at);

CREATE TABLE outbox_events (
    id           uuid        PRIMARY KEY,
    aggregate_id uuid        NOT NULL,
    event_type   varchar(100) NOT NULL,
    payload      text        NOT NULL,
    created_at   timestamptz NOT NULL,
    published_at timestamptz
);

CREATE INDEX ix_outbox_unpublished ON outbox_events (published_at, created_at);
