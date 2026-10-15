ALTER TYPE notification_type ADD VALUE IF NOT EXISTS 'OFFER_PUBLISHED';

ALTER TABLE notification_requested
    ALTER COLUMN demand_published_requested_id DROP NOT NULL,
    ALTER COLUMN seller_id DROP NOT NULL,
    ALTER COLUMN demand_id DROP NOT NULL,
    ADD COLUMN researcher_id VARCHAR(255),
    ADD COLUMN offer_id VARCHAR(255),
    ADD CONSTRAINT fk_notification_requested_researcher
        FOREIGN KEY (researcher_id) REFERENCES researchers (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_notification_requested_offer
        FOREIGN KEY (offer_id) REFERENCES offers (id) ON DELETE CASCADE,
    ADD CONSTRAINT ck_notification_requested_target CHECK (
        (demand_published_requested_id IS NOT NULL AND seller_id IS NOT NULL
            AND demand_id IS NOT NULL AND researcher_id IS NULL AND offer_id IS NULL)
        OR
        (demand_published_requested_id IS NULL AND seller_id IS NULL
            AND demand_id IS NULL AND researcher_id IS NOT NULL AND offer_id IS NOT NULL)
    );

CREATE INDEX idx_notification_requested_offer_id ON notification_requested (offer_id);
CREATE INDEX idx_notification_requested_researcher_id ON notification_requested (researcher_id);
