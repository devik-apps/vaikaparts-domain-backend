ALTER TABLE demands ADD COLUMN creation_request_key VARCHAR(36);
ALTER TABLE offers ADD COLUMN creation_request_key VARCHAR(36);

CREATE UNIQUE INDEX uq_demands_creation_request_key
    ON demands (creation_request_key);
CREATE UNIQUE INDEX uq_offers_creation_request_key
    ON offers (creation_request_key);
