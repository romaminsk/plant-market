-- Optimistic locking for admin writes that can race with reservations.
ALTER TABLE products ADD COLUMN version bigint NOT NULL DEFAULT 0;
ALTER TABLE product_variants ADD COLUMN version bigint NOT NULL DEFAULT 0;
ALTER TABLE individual_plants ADD COLUMN version bigint NOT NULL DEFAULT 0;
ALTER TABLE stock_batches ADD COLUMN version bigint NOT NULL DEFAULT 0;
