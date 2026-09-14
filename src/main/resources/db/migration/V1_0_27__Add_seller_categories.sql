-- Preserve notifications for existing sellers until they choose specific categories.
ALTER TABLE sellers
    ADD COLUMN handle_all_category BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE seller_categories
(
    seller_id VARCHAR(255) NOT NULL REFERENCES sellers (id) ON DELETE CASCADE,
    part_category VARCHAR(255) NOT NULL
);

CREATE INDEX idx_seller_categories_seller_id ON seller_categories (seller_id);

ALTER TYPE post_status ADD VALUE 'CLOSED';