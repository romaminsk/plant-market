-- Phase 2: payment records for the sandbox provider.
CREATE TABLE payments (
    id            uuid        PRIMARY KEY,
    order_id      uuid        NOT NULL,
    amount_minor  bigint      NOT NULL CHECK (amount_minor > 0),
    currency      varchar(3)  NOT NULL,
    status        varchar(20) NOT NULL,
    provider_ref  varchar(128),
    redirect_url  varchar(1000),
    created_at    timestamptz NOT NULL,
    updated_at    timestamptz NOT NULL,
    version       bigint      NOT NULL DEFAULT 0
);

CREATE INDEX ix_payments_order ON payments (order_id);
CREATE UNIQUE INDEX ux_payments_provider_ref ON payments (provider_ref);
