-- Keep both historical demand children and historical parentless offer rows.
-- New offer children now share the demand publication parent/child pipeline.
ALTER TABLE notification_requested
    DROP CONSTRAINT ck_notification_requested_target,
    ADD CONSTRAINT ck_notification_requested_target CHECK (
        (demand_published_requested_id IS NOT NULL AND seller_id IS NOT NULL
            AND demand_id IS NOT NULL AND researcher_id IS NULL AND offer_id IS NULL)
        OR
        (seller_id IS NULL AND demand_id IS NULL
            AND researcher_id IS NOT NULL AND offer_id IS NOT NULL)
    );
