CREATE TABLE offers
(
    id           VARCHAR(255) PRIMARY KEY,
    description  TEXT,
    created_at   TIMESTAMP,
    updated_at   TIMESTAMP,
    canceled_at  TIMESTAMP,
    suspended_at TIMESTAMP,
    offer_status post_status  NOT NULL,
    seller_id    VARCHAR(255) NOT NULL,
    CONSTRAINT fk_offer_seller FOREIGN KEY (seller_id) REFERENCES sellers (id)
);

CREATE INDEX idx_offers_seller_id ON offers (seller_id);
CREATE INDEX idx_offers_status ON offers (offer_status);
CREATE INDEX idx_offers_created_at ON offers (created_at);
