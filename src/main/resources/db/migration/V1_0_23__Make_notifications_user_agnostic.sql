ALTER TABLE notifications
    RENAME COLUMN seller_id TO recipient_user_id;

ALTER TABLE notifications
    ALTER COLUMN notification_requested_id DROP NOT NULL,
    ALTER COLUMN demand_id DROP NOT NULL,
    ADD COLUMN offer_id VARCHAR(255),
    ADD CONSTRAINT fk_notification_offer
        FOREIGN KEY (offer_id) REFERENCES offers (id) ON DELETE CASCADE;

CREATE INDEX idx_notifications_offer_id ON notifications (offer_id);

ALTER INDEX idx_notifications_seller_id RENAME TO idx_notifications_recipient_id;
ALTER INDEX idx_notifications_seller_unread RENAME TO idx_notifications_recipient_unread;
