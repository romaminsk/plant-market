-- Phase 2: restock subscriptions (J2).
CREATE TABLE restock_subscriptions (
    id          uuid         PRIMARY KEY,
    target_type varchar(20)  NOT NULL,
    target_id   uuid         NOT NULL,
    email       varchar(254) NOT NULL,
    status      varchar(20)  NOT NULL,
    created_at  timestamptz  NOT NULL,
    notified_at timestamptz,
    CONSTRAINT uq_restock_subscription UNIQUE (target_type, target_id, email)
);

CREATE INDEX ix_restock_subscriptions_target
    ON restock_subscriptions (target_type, target_id, status);
