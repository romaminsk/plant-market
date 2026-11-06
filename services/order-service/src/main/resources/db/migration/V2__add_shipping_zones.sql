-- Phase 2: delivery zones and shipping fields on orders.
ALTER TABLE orders ADD COLUMN customer_email varchar(254);
ALTER TABLE orders ADD COLUMN shipping_zone_id uuid;
ALTER TABLE orders ADD COLUMN thermal_pack boolean NOT NULL DEFAULT false;

CREATE TABLE shipping_zones (
    id                     uuid         PRIMARY KEY,
    name                   varchar(120) NOT NULL,
    city                   varchar(120) NOT NULL,
    price_minor            bigint       NOT NULL CHECK (price_minor >= 0),
    free_from_minor        bigint       NOT NULL DEFAULT 0 CHECK (free_from_minor >= 0),
    min_days               integer      NOT NULL CHECK (min_days >= 0),
    max_days               integer      NOT NULL CHECK (max_days >= min_days),
    winter_requires_thermal boolean     NOT NULL DEFAULT false,
    thermal_pack_minor     bigint       NOT NULL DEFAULT 0 CHECK (thermal_pack_minor >= 0),
    active                 boolean      NOT NULL DEFAULT true,
    created_at             timestamptz  NOT NULL,
    updated_at             timestamptz  NOT NULL
);

CREATE INDEX ix_shipping_zones_city ON shipping_zones (city);
