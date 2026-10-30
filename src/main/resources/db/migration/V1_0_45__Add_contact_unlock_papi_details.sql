ALTER TABLE contact_unlocks
    ADD COLUMN payment_method VARCHAR(50),
    ADD COLUMN payment_amount NUMERIC(12, 2);
