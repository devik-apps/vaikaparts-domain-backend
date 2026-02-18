CREATE TABLE demand_photos
(
    demand_id  VARCHAR(255) NOT NULL,
    bucket_key TEXT         NOT NULL,
    CONSTRAINT fk_demand_photos_demand FOREIGN KEY (demand_id) REFERENCES demands (id) ON DELETE CASCADE
);

CREATE INDEX idx_demand_photos_demand_id ON demand_photos (demand_id);
