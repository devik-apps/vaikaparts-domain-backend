ALTER TABLE car_parts
    ADD CONSTRAINT uk_car_parts_demand_id UNIQUE (demand_id);

ALTER TABLE offer_part_infos
    ADD CONSTRAINT uk_offer_part_infos_offer_id UNIQUE (offer_id);

COMMENT ON CONSTRAINT uk_car_parts_demand_id ON car_parts IS
    'Enforces one-to-one relationship: each demand has exactly one part';

COMMENT ON CONSTRAINT uk_offer_part_infos_offer_id ON offer_part_infos IS
    'Enforces one-to-one relationship: each offer has exactly one part info';

DROP INDEX IF EXISTS idx_car_parts_demand_id;
CREATE INDEX idx_car_parts_demand_id ON car_parts (demand_id) WHERE demand_id IS NOT NULL;

DROP INDEX IF EXISTS idx_offer_part_infos_offer_id;
CREATE INDEX idx_offer_part_infos_offer_id ON offer_part_infos (offer_id) WHERE offer_id IS NOT NULL;