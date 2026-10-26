ALTER TABLE external_notification_deliveries
    DROP CONSTRAINT external_notification_deliveries_channel_check;

ALTER TABLE external_notification_deliveries
    ADD CONSTRAINT external_notification_deliveries_channel_check
        CHECK (channel IN ('EMAIL', 'SMS', 'WEB_PUSH'));
