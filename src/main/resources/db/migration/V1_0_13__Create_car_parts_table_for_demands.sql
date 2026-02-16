CREATE TABLE car_parts
(
    id            VARCHAR(255) PRIMARY KEY,
    part_name     VARCHAR(255)  NOT NULL,
    car_brand     VARCHAR(255)  NOT NULL,
    car_model     VARCHAR(255)  NOT NULL,
    car_year      INTEGER       NOT NULL,
    part_category part_category NOT NULL,
    demand_id     VARCHAR(255)  NOT NULL,
    CONSTRAINT fk_car_parts_demand FOREIGN KEY (demand_id) REFERENCES demands (id) ON DELETE CASCADE
);

CREATE TABLE part_photos
(
    part_id      VARCHAR(255) NOT NULL,
    image_bucket TEXT         NOT NULL,
    CONSTRAINT fk_part_photos FOREIGN KEY (part_id) REFERENCES car_parts (id) ON DELETE CASCADE
);

CREATE INDEX idx_part_photos_id ON part_photos (part_id);
CREATE INDEX idx_car_parts_demand_id ON car_parts (demand_id);
CREATE INDEX idx_car_parts_category ON car_parts (part_category);
CREATE INDEX idx_car_parts_brand_model ON car_parts (car_brand, car_model);