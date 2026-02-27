CREATE TABLE demand_published_requested
(
    id                       VARCHAR(255) PRIMARY KEY,
    demand_id                VARCHAR(255)   NOT NULL,
    status                   process_status NOT NULL,
    attempt_nb               INTEGER        NOT NULL DEFAULT 0,
    error_message            TEXT,
    total_sellers_to_notify  INTEGER        NOT NULL,
    notifications_sent_count INTEGER        NOT NULL DEFAULT 0,
    created_at               TIMESTAMP      NOT NULL,
    updated_at               TIMESTAMP      NOT NULL,
    completed_at             TIMESTAMP,

    CONSTRAINT fk_demand_published_demand FOREIGN KEY (demand_id) REFERENCES demands (id) ON DELETE CASCADE
);

CREATE INDEX idx_demand_published_logs_demand_id ON demand_published_requested (demand_id);
CREATE INDEX idx_demand_published_logs_status ON demand_published_requested (status);
CREATE INDEX idx_demand_published_logs_created_at ON demand_published_requested (created_at DESC);