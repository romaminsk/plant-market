-- Phase 3: delivery address for shipped orders.
ALTER TABLE orders ADD COLUMN delivery_address varchar(500);
