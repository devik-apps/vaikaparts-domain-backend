CREATE TYPE contact_unlock_status AS ENUM ('PENDING', 'RELEASED', 'FAILED', 'EXPIRED');

CREATE TABLE contact_unlocks
(
    unlock_request_id VARCHAR(255) PRIMARY KEY,
    offer_id          VARCHAR(255)             NOT NULL REFERENCES offers (id),
    buyer_id          VARCHAR(255)             NOT NULL REFERENCES researchers (id),
    seller_id         VARCHAR(255)             NOT NULL REFERENCES sellers (id),
    payment_id        VARCHAR(255) UNIQUE,
    release_event_id  VARCHAR(255) UNIQUE,
    provider          VARCHAR(50)              NOT NULL,
    status            contact_unlock_status    NOT NULL,
    payment_url       TEXT,
    paid_at           TIMESTAMP WITH TIME ZONE,
    released_at       TIMESTAMP WITH TIME ZONE,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX uq_active_contact_unlock
    ON contact_unlocks (buyer_id, offer_id)
    WHERE status IN ('PENDING', 'RELEASED');

CREATE INDEX idx_contact_unlock_offer ON contact_unlocks (offer_id);
CREATE INDEX idx_contact_unlock_seller ON contact_unlocks (seller_id);
