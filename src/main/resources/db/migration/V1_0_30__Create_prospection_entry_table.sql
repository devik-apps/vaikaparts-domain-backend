CREATE TYPE prospector_type AS ENUM
    ('ELITINA', 'ANDOMAMY', 'DIEUDONNE', 'JONATHAN', 'KYLE');

CREATE TABLE prospection_entries
(
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id                VARCHAR(255) NOT NULL,
    prospector               prospector_type NOT NULL,
    prospection_zone         VARCHAR(120) NOT NULL,
    demonstration_completed  BOOLEAN NOT NULL DEFAULT FALSE,
    consent_confirmed        BOOLEAN NOT NULL DEFAULT FALSE,
    confirmation_status      VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    prospector_notes         TEXT,
    confirmation_notes       TEXT,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    confirmed_at             TIMESTAMPTZ,

    CONSTRAINT fk_prospection_seller
        FOREIGN KEY (seller_id) REFERENCES sellers (id) ON DELETE CASCADE,
    CONSTRAINT uk_prospection_seller UNIQUE (seller_id),
    CONSTRAINT ck_prospection_status CHECK
        (confirmation_status IN ('PENDING', 'CONFIRMED', 'UNREACHABLE', 'REJECTED')),
    CONSTRAINT ck_prospection_zone_not_blank CHECK
        (length(trim(prospection_zone)) >= 2),
    CONSTRAINT ck_prospection_consent CHECK
        (consent_confirmed = TRUE),
    CONSTRAINT ck_prospection_confirmed_at CHECK
        (
        (confirmation_status = 'CONFIRMED' AND confirmed_at IS NOT NULL)
            OR
        (confirmation_status <> 'CONFIRMED' AND confirmed_at IS NULL)
        )
);

CREATE INDEX idx_prospection_entries_prospector
    ON prospection_entries (prospector);
CREATE INDEX idx_prospection_entries_status
    ON prospection_entries (confirmation_status);
CREATE INDEX idx_prospection_entries_created_at
    ON prospection_entries (created_at DESC);
CREATE INDEX idx_prospection_entries_zone
    ON prospection_entries (prospection_zone);

ALTER TABLE prospection_entries ENABLE ROW LEVEL SECURITY;
