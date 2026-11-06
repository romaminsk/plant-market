-- Phase 3: opt-in care reminders for purchased plants.
CREATE TABLE care_reminders (
    id            uuid         PRIMARY KEY,
    order_id      uuid         NOT NULL REFERENCES orders (id),
    order_item_id uuid         NOT NULL REFERENCES order_items (id),
    customer_id   uuid         NOT NULL,
    customer_email varchar(254) NOT NULL,
    plant_name    varchar(200) NOT NULL,
    interval_days integer      NOT NULL CHECK (interval_days BETWEEN 1 AND 365),
    next_send_at  timestamptz  NOT NULL,
    active        boolean      NOT NULL DEFAULT true,
    created_at    timestamptz  NOT NULL,
    updated_at    timestamptz  NOT NULL
);

CREATE INDEX ix_care_reminders_due ON care_reminders (active, next_send_at);
CREATE UNIQUE INDEX uq_care_reminders_item ON care_reminders (order_item_id) WHERE active;
