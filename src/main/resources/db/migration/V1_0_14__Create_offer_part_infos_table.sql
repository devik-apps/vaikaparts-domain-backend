CREATE TABLE offer_part_infos
(
    id                VARCHAR(255) PRIMARY KEY,
    part_name         VARCHAR(255)     NOT NULL,
    car_brand         VARCHAR(255)     NOT NULL,
    car_model         VARCHAR(255)     NOT NULL,
    car_year          INTEGER          NOT NULL,
    part_image_bucket TEXT,
    part_category     part_category    NOT NULL,
    description       TEXT,
    price             DOUBLE PRECISION NOT NULL,
    condition         part_condition   NOT NULL,
    offer_id          VARCHAR(255)     NOT NULL,
    CONSTRAINT fk_offer_part_infos_offer FOREIGN KEY (offer_id) REFERENCES offers (id) ON DELETE CASCADE
);

CREATE INDEX idx_offer_part_infos_offer_id ON offer_part_infos (offer_id);
CREATE INDEX idx_offer_part_infos_category ON offer_part_infos (part_category);
CREATE INDEX idx_offer_part_infos_condition ON offer_part_infos (condition);
CREATE INDEX idx_offer_part_infos_brand_model ON offer_part_infos (car_brand, car_model);
CREATE INDEX idx_offer_part_infos_price ON offer_part_infos (price);