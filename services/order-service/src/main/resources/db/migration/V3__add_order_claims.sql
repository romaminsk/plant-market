-- Phase 2: damaged plant claims with photos and operator decision.
CREATE TABLE order_claims (
    id               uuid          PRIMARY KEY,
    order_id         uuid          NOT NULL REFERENCES orders (id),
    customer_id      uuid          NOT NULL,
    reason           varchar(2000) NOT NULL,
    status           varchar(20)   NOT NULL,
    decision_comment varchar(2000),
    created_at       timestamptz   NOT NULL,
    updated_at       timestamptz   NOT NULL,
    decided_at       timestamptz
);

CREATE INDEX ix_order_claims_order ON order_claims (order_id);

CREATE TABLE order_claim_photos (
    claim_id uuid          NOT NULL REFERENCES order_claims (id) ON DELETE CASCADE,
    position integer       NOT NULL,
    url      varchar(1000) NOT NULL,
    PRIMARY KEY (claim_id, position)
);
