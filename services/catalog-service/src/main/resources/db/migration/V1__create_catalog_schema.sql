-- Phase 0 catalog schema. One database per service: pm_catalog.
CREATE TABLE categories (
    id          uuid         PRIMARY KEY,
    name        varchar(120) NOT NULL,
    slug        varchar(120) NOT NULL UNIQUE,
    description varchar(2000),
    created_at  timestamptz  NOT NULL,
    updated_at  timestamptz  NOT NULL
);

CREATE TABLE products (
    id          uuid         PRIMARY KEY,
    sku         varchar(64)  NOT NULL UNIQUE,
    name        varchar(200) NOT NULL,
    slug        varchar(200) NOT NULL UNIQUE,
    description varchar(5000),
    category_id uuid         NOT NULL REFERENCES categories (id),
    price_minor bigint       NOT NULL CHECK (price_minor >= 0),
    currency    varchar(3)   NOT NULL,
    status      varchar(20)  NOT NULL,
    care_level  varchar(20)  NOT NULL,
    light       varchar(20)  NOT NULL,
    pet_safe    boolean      NOT NULL,
    created_at  timestamptz  NOT NULL,
    updated_at  timestamptz  NOT NULL
);

CREATE INDEX ix_products_category ON products (category_id);
CREATE INDEX ix_products_status ON products (status);
CREATE INDEX ix_products_name_lower ON products (lower(name));
