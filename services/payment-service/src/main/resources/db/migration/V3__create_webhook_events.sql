-- Replay protection: a provider event id is applied at most once.
CREATE TABLE payment_webhook_events (
    event_id    varchar(128) PRIMARY KEY,
    received_at timestamptz  NOT NULL
);
