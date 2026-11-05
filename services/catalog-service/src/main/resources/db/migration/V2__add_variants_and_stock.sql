-- Phase 1: variants, individual plants and stock batches.
ALTER TABLE products ADD COLUMN variegated boolean NOT NULL DEFAULT false;
ALTER TABLE products ADD COLUMN size varchar(32);
ALTER TABLE products ADD COLUMN care_guide varchar(5000);

CREATE TABLE product_variants (
    id          uuid         PRIMARY KEY,
    product_id  uuid         NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    sku         varchar(64)  NOT NULL UNIQUE,
    name        varchar(120) NOT NULL,
    size        varchar(32),
    pot_size    varchar(32),
    form        varchar(32),
    price_minor bigint       NOT NULL CHECK (price_minor >= 0),
    currency    varchar(3)   NOT NULL,
    status      varchar(20)  NOT NULL,
    created_at  timestamptz  NOT NULL,
    updated_at  timestamptz  NOT NULL
);

CREATE INDEX ix_product_variants_product ON product_variants (product_id);

CREATE TABLE individual_plants (
    id          uuid         PRIMARY KEY,
    product_id  uuid         NOT NULL REFERENCES products (id),
    code        varchar(64)  NOT NULL UNIQUE,
    description varchar(5000),
    photo_url   varchar(1000),
    price_minor bigint       NOT NULL CHECK (price_minor >= 0),
    currency    varchar(3)   NOT NULL,
    status      varchar(20)  NOT NULL,
    care_level  varchar(20)  NOT NULL,
    light       varchar(20)  NOT NULL,
    pet_safe    boolean      NOT NULL,
    created_at  timestamptz  NOT NULL,
    updated_at  timestamptz  NOT NULL
);

CREATE INDEX ix_individual_plants_product ON individual_plants (product_id);
CREATE INDEX ix_individual_plants_status ON individual_plants (status);

CREATE TABLE stock_batches (
    id                 uuid         PRIMARY KEY,
    product_variant_id uuid         NOT NULL REFERENCES product_variants (id),
    supplier           varchar(200) NOT NULL,
    quantity           integer      NOT NULL CHECK (quantity > 0),
    remaining_quantity integer      NOT NULL CHECK (remaining_quantity >= 0),
    unit_cost_minor    bigint       NOT NULL CHECK (unit_cost_minor >= 0),
    currency           varchar(3)   NOT NULL,
    received_at        timestamptz  NOT NULL,
    quarantine_until   timestamptz  NOT NULL,
    status             varchar(20)  NOT NULL,
    created_at         timestamptz  NOT NULL,
    updated_at         timestamptz  NOT NULL
);

CREATE INDEX ix_stock_batches_variant ON stock_batches (product_variant_id);
CREATE INDEX ix_stock_batches_status ON stock_batches (status);
