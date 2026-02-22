CREATE TABLE notification_requested
(
    id                            VARCHAR(255) PRIMARY KEY,
    demand_published_requested_id VARCHAR(255)      NOT NULL,
    seller_id                     VARCHAR(255)      NOT NULL,
    demand_id                     VARCHAR(255)      NOT NULL,
    notification_type             notification_type NOT NULL,
    status                        process_status    NOT NULL,
    attempt_nb                    INTEGER           NOT NULL DEFAULT 0,
    error_message                 TEXT,
    created_at                    TIMESTAMP         NOT NULL,
    updated_at                    TIMESTAMP         NOT NULL,
    completed_at                  TIMESTAMP,

    CONSTRAINT fk_notification_requested_demand_published
        FOREIGN KEY (demand_published_requested_id)
            REFERENCES demand_published_requested (id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_requested_seller
        FOREIGN KEY (seller_id)
            REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_requested_demand
        FOREIGN KEY (demand_id)
            REFERENCES demands (id) ON DELETE CASCADE
);

CREATE INDEX idx_notification_requested_seller_id ON notification_requested (seller_id);
CREATE INDEX idx_notification_requested_demand_id ON notification_requested (demand_id);
CREATE INDEX idx_notification_requested_status ON notification_requested (status);
CREATE INDEX idx_notification_requested_parent ON notification_requested (demand_published_requested_id);
CREATE INDEX idx_notification_requested_created_at ON notification_requested (created_at DESC);