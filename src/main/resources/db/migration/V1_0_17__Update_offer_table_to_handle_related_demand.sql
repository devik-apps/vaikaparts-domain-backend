ALTER TABLE offers
    ADD COLUMN demand_id VARCHAR(255) NOT NULL default '',
    ADD CONSTRAINT fk_offer_demand FOREIGN KEY (demand_id) REFERENCES demands (id);

CREATE INDEX idx_offers_demand_id ON offers (demand_id);