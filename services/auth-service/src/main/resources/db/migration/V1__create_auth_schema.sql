-- Phase 0 auth schema. One database per service: pm_auth.
CREATE TABLE users (
    id            uuid         PRIMARY KEY,
    email         varchar(254) NOT NULL,
    password_hash varchar(100) NOT NULL,
    full_name     varchar(200) NOT NULL,
    status        varchar(20)  NOT NULL,
    created_at    timestamptz  NOT NULL,
    updated_at    timestamptz  NOT NULL
);

CREATE UNIQUE INDEX ux_users_email_lower ON users (lower(email));

CREATE TABLE roles (
    name varchar(32) PRIMARY KEY
);

INSERT INTO roles (name)
VALUES ('CUSTOMER'), ('OPERATOR'), ('MANAGER'), ('ADMIN'), ('SYSTEM');

CREATE TABLE user_roles (
    user_id   uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_name varchar(32) NOT NULL REFERENCES roles (name),
    PRIMARY KEY (user_id, role_name)
);

CREATE TABLE refresh_tokens (
    id         uuid        PRIMARY KEY,
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash varchar(64) NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    created_at timestamptz NOT NULL
);

CREATE INDEX ix_refresh_tokens_user ON refresh_tokens (user_id);
