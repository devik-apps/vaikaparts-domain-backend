CREATE TYPE arrondissement AS ENUM ('FIRST', 'SECOND', 'THIRD', 'FOURTH', 'FIFTH', 'SIXTH');

ALTER TABLE sellers
    ADD COLUMN arrondissement arrondissement;

CREATE INDEX idx_sellers_arrondissement ON sellers (arrondissement);
