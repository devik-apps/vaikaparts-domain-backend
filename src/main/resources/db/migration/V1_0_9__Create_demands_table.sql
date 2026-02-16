CREATE TABLE demands
(
    id            VARCHAR(255) PRIMARY KEY,
    description   TEXT,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    canceled_at   TIMESTAMP,
    suspended_at  TIMESTAMP,
    demand_status post_status  NOT NULL,
    researcher_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_demand_researcher FOREIGN KEY (researcher_id) REFERENCES researchers (id)
);

CREATE INDEX idx_demands_researcher_id ON demands (researcher_id);
CREATE INDEX idx_demands_status ON demands (demand_status);
CREATE INDEX idx_demands_created_at ON demands (created_at);