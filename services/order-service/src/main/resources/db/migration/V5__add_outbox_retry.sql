-- Phase 3: outbox retry with exponential backoff and dead-letter flag.
ALTER TABLE outbox_events ADD COLUMN attempts integer NOT NULL DEFAULT 0;
ALTER TABLE outbox_events ADD COLUMN last_error varchar(500);
ALTER TABLE outbox_events ADD COLUMN next_attempt_at timestamptz;
ALTER TABLE outbox_events ADD COLUMN dead boolean NOT NULL DEFAULT false;

CREATE INDEX ix_outbox_events_pending ON outbox_events (published_at, next_attempt_at);
