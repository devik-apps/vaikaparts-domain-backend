CREATE TYPE verification_status AS ENUM ('PENDING', 'SUCCESS', 'FAILED');
CREATE TYPE payment_type AS ENUM ('UNLOCK_PROFILE');

CREATE TABLE payments
(
    transaction_id VARCHAR(45) PRIMARY KEY,
    researcher_id  VARCHAR(45)         NOT NULL,
    related_offer  VARCHAR(45)         NOT NULL,
    status         verification_status NOT NULL DEFAULT 'PENDING',
    payment_type   payment_type        NOT NULL,
    payer_msisdn   VARCHAR(45)         NOT NULL,

    CONSTRAINT fk_researcher_id FOREIGN KEY (researcher_id) REFERENCES researchers (id),
    CONSTRAINT fk_related_offer FOREIGN KEY (related_offer) REFERENCES offers (id)
);