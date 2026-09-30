-- Email and SMS are opt-in for all user types; IN_APP remains always available.
ALTER TABLE users
    ADD COLUMN email_notifications_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN sms_notifications_enabled BOOLEAN NOT NULL DEFAULT FALSE;
