CREATE TABLE researchers
(
    id      VARCHAR(255) PRIMARY KEY,
    city    city   NOT NULL,
    region  region NOT NULL,
    address TEXT   NOT NULL,

    CONSTRAINT fk_researchers_users FOREIGN KEY (id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_researchers_city ON researchers (city);
CREATE INDEX idx_researchers_region ON researchers (region);