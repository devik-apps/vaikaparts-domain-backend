CREATE TABLE notifications
(
    id                        VARCHAR(255) PRIMARY KEY,
    notification_requested_id VARCHAR(255)      NOT NULL,
    seller_id                 VARCHAR(255)      NOT NULL,
    demand_id                 VARCHAR(255)      NOT NULL,
    message                   TEXT              NOT NULL,
    notification_type         notification_type NOT NULL,
    is_read                   BOOLEAN           NOT NULL DEFAULT FALSE,
    click_action              TEXT,
    created_at                TIMESTAMP         NOT NULL,
    read_at                   TIMESTAMP,

    CONSTRAINT fk_notification_requested
        FOREIGN KEY (notification_requested_id)
            REFERENCES notification_requested (id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_seller
        FOREIGN KEY (seller_id)
            REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_demand
        FOREIGN KEY (demand_id)
            REFERENCES demands (id) ON DELETE CASCADE
);

CREATE INDEX idx_notifications_seller_id ON notifications (seller_id);
CREATE INDEX idx_notifications_is_read ON notifications (seller_id, is_read);
CREATE INDEX idx_notifications_created_at ON notifications (created_at DESC);
CREATE INDEX idx_notifications_demand_id ON notifications (demand_id);
CREATE INDEX idx_notifications_seller_unread ON notifications (seller_id, is_read) WHERE is_read = false;