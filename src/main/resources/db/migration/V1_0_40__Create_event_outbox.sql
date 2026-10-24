CREATE TABLE event_outbox
(
    id              VARCHAR(255) PRIMARY KEY,
    event_type      VARCHAR(255) NOT NULL,
    payload         TEXT         NOT NULL,
    attempt_nb      INTEGER      NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP    NOT NULL,
    last_error      TEXT,
    created_at      TIMESTAMP    NOT NULL,
    published_at    TIMESTAMP,
    locked_at       TIMESTAMP,
    lock_token      VARCHAR(255)
);

CREATE INDEX idx_event_outbox_due
    ON event_outbox (next_attempt_at, created_at)
    WHERE published_at IS NULL;

CREATE INDEX idx_event_outbox_lock_token
    ON event_outbox (lock_token)
    WHERE lock_token IS NOT NULL;
