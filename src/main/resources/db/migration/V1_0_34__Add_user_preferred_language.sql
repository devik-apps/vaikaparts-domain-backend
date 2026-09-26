CREATE TYPE user_language AS ENUM ('FR', 'MG', 'EN');

ALTER TABLE users
    ADD COLUMN preferred_language user_language NOT NULL DEFAULT 'FR';
