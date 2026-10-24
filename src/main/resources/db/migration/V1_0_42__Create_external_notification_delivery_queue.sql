CREATE TABLE external_notification_deliveries
(
    id              VARCHAR(255) PRIMARY KEY,
    notification_id VARCHAR(255) NOT NULL REFERENCES notifications (id) ON DELETE CASCADE,
    channel         VARCHAR(16)  NOT NULL CHECK (channel IN ('EMAIL', 'SMS')),
    status          process_status NOT NULL DEFAULT 'PENDING',
    attempt_nb      INTEGER      NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP    NOT NULL,
    last_error      TEXT,
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL,
    delivered_at    TIMESTAMP,
    locked_at       TIMESTAMP,
    lock_token      VARCHAR(255),
    CONSTRAINT uq_external_notification_delivery UNIQUE (notification_id, channel)
);

CREATE INDEX idx_external_notification_deliveries_due
    ON external_notification_deliveries (next_attempt_at, created_at)
    WHERE status <> 'SUCCESS';

CREATE INDEX idx_external_notification_deliveries_lock_token
    ON external_notification_deliveries (lock_token)
    WHERE lock_token IS NOT NULL;
