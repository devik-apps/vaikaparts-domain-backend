ALTER TABLE contact_unlocks
    ADD COLUMN payer_fee NUMERIC(12, 2),
    ADD COLUMN total_amount NUMERIC(12, 2);
