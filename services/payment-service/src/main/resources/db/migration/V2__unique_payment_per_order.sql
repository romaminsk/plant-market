-- One payment record per order: retries return the existing payment instead of charging twice.
CREATE UNIQUE INDEX ux_payments_order ON payments (order_id);
