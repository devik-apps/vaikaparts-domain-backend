CREATE TABLE sellers
(
    id          VARCHAR(255) PRIMARY KEY,
    garage_name VARCHAR(255) NOT NULL,
    city        city         NOT NULL,
    region      region       NOT NULL,
    address     TEXT         NOT NULL,
    latitude    DOUBLE PRECISION,
    longitude   DOUBLE PRECISION,

    CONSTRAINT fk_sellers_users FOREIGN KEY (id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_sellers_city ON sellers (city);
CREATE INDEX idx_sellers_region ON sellers (region);
CREATE INDEX idx_sellers_location ON sellers (latitude, longitude);