-- Phase 2: stock reservations with TTL.
ALTER TABLE product_variants
    ADD COLUMN reserved_quantity integer NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0);

CREATE TABLE reservations (
    id          uuid         PRIMARY KEY,
    order_id    uuid         NOT NULL,
    target_type varchar(20)  NOT NULL,
    target_id   uuid         NOT NULL,
    quantity    integer      NOT NULL CHECK (quantity > 0),
    status      varchar(20)  NOT NULL,
    expires_at  timestamptz  NOT NULL,
    created_at  timestamptz  NOT NULL,
    updated_at  timestamptz  NOT NULL
);

CREATE INDEX ix_reservations_order ON reservations (order_id);
CREATE INDEX ix_reservations_status_expires ON reservations (status, expires_at);
