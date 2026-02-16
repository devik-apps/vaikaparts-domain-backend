CREATE TABLE offer_photos
(
    offer_id   VARCHAR(255) NOT NULL,
    bucket_key TEXT         NOT NULL,
    CONSTRAINT fk_offer_photos_offer FOREIGN KEY (offer_id) REFERENCES offers (id) ON DELETE CASCADE
);

CREATE INDEX idx_offer_photos_offer_id ON offer_photos (offer_id);