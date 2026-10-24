WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY notification_requested_id
               ORDER BY created_at DESC, id DESC
           ) AS row_number,
           BOOL_OR(is_read) OVER (PARTITION BY notification_requested_id) AS was_read,
           MAX(read_at) OVER (PARTITION BY notification_requested_id) AS latest_read_at
    FROM notifications
    WHERE notification_requested_id IS NOT NULL
), merged AS (
    SELECT id, was_read, latest_read_at
    FROM ranked
    WHERE row_number = 1
)
UPDATE notifications notification
SET is_read = merged.was_read,
    read_at = merged.latest_read_at
FROM merged
WHERE notification.id = merged.id;

WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY notification_requested_id
               ORDER BY created_at DESC, id DESC
           ) AS row_number
    FROM notifications
    WHERE notification_requested_id IS NOT NULL
)
DELETE FROM notifications notification
USING ranked
WHERE notification.id = ranked.id
  AND ranked.row_number > 1;

WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY offer_notification_requested_id
               ORDER BY created_at DESC, id DESC
           ) AS row_number,
           BOOL_OR(is_read) OVER (PARTITION BY offer_notification_requested_id) AS was_read,
           MAX(read_at) OVER (PARTITION BY offer_notification_requested_id) AS latest_read_at
    FROM notifications
    WHERE offer_notification_requested_id IS NOT NULL
), merged AS (
    SELECT id, was_read, latest_read_at
    FROM ranked
    WHERE row_number = 1
)
UPDATE notifications notification
SET is_read = merged.was_read,
    read_at = merged.latest_read_at
FROM merged
WHERE notification.id = merged.id;

WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY offer_notification_requested_id
               ORDER BY created_at DESC, id DESC
           ) AS row_number
    FROM notifications
    WHERE offer_notification_requested_id IS NOT NULL
)
DELETE FROM notifications notification
USING ranked
WHERE notification.id = ranked.id
  AND ranked.row_number > 1;

CREATE UNIQUE INDEX uq_notifications_notification_requested
    ON notifications (notification_requested_id)
    WHERE notification_requested_id IS NOT NULL;

CREATE UNIQUE INDEX uq_notifications_offer_notification_requested
    ON notifications (offer_notification_requested_id)
    WHERE offer_notification_requested_id IS NOT NULL;
