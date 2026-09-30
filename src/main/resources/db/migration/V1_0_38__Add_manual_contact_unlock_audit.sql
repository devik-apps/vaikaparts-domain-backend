ALTER TABLE contact_unlocks
    ADD COLUMN reviewed_by VARCHAR(255),
    ADD COLUMN reviewed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN review_note TEXT,
    ADD COLUMN manual_payment_reference VARCHAR(255);

DROP INDEX uq_active_contact_unlock;

CREATE UNIQUE INDEX uq_active_contact_unlock
    ON contact_unlocks (buyer_id, offer_id)
    WHERE status IN ('PENDING', 'PENDING_MANUAL_REVIEW', 'RELEASED');
