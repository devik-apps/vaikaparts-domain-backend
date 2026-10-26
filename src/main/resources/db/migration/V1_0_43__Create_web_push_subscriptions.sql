CREATE TABLE web_push_subscriptions
(
    id         VARCHAR(255) PRIMARY KEY,
    user_id    VARCHAR(255) NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    endpoint   VARCHAR(2048) NOT NULL UNIQUE,
    p256dh     VARCHAR(512) NOT NULL,
    auth       VARCHAR(512) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_web_push_subscriptions_user_id ON web_push_subscriptions (user_id);
