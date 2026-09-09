CREATE TABLE offer_notification_requested
(
    id VARCHAR(255) PRIMARY KEY,
    researcher_id VARCHAR(255) NOT NULL REFERENCES researchers (id) ON DELETE CASCADE,
    offer_id VARCHAR(255) NOT NULL REFERENCES offers (id) ON DELETE CASCADE,
    notification_type notification_type NOT NULL,
    status process_status NOT NULL,
    attempt_nb INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP
);

CREATE INDEX idx_offer_notification_requested_offer ON offer_notification_requested (offer_id);
CREATE INDEX idx_offer_notification_requested_researcher ON offer_notification_requested (researcher_id);
CREATE INDEX idx_offer_notification_requested_status ON offer_notification_requested (status);

ALTER TABLE notifications
    ADD COLUMN offer_notification_requested_id VARCHAR(255),
    ADD CONSTRAINT fk_notification_offer_requested
        FOREIGN KEY (offer_notification_requested_id)
            REFERENCES offer_notification_requested (id) ON DELETE CASCADE,
    ADD CONSTRAINT ck_notification_single_request
        CHECK (notification_requested_id IS NULL OR offer_notification_requested_id IS NULL);

CREATE INDEX idx_notifications_offer_requested ON notifications (offer_notification_requested_id);

-- Preserve offer history created by V1_0_24 and move its notification links.
-- Original tracking rows are retained; no historic data is deleted.
INSERT INTO offer_notification_requested
    (id, researcher_id, offer_id, notification_type, status, attempt_nb,
     error_message, created_at, updated_at, completed_at)
SELECT id, researcher_id, offer_id, notification_type, status, attempt_nb,
       error_message, created_at, updated_at, completed_at
FROM notification_requested
WHERE offer_id IS NOT NULL AND researcher_id IS NOT NULL
    AND notification_type = 'OFFER_PUBLISHED';

UPDATE notifications n
SET offer_notification_requested_id = r.id, notification_requested_id = NULL
FROM offer_notification_requested r
WHERE n.notification_requested_id = r.id;
