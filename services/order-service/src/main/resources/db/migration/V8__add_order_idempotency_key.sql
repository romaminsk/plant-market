-- Client retries with the same Idempotency-Key must return the same order.
ALTER TABLE orders ADD COLUMN idempotency_key varchar(128);
CREATE UNIQUE INDEX ux_orders_idempotency_key ON orders (idempotency_key)
    WHERE idempotency_key IS NOT NULL;
