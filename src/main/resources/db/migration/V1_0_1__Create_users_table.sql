CREATE TABLE users
(
    id               VARCHAR(255) PRIMARY KEY,
    supabase_user_id VARCHAR(255) NOT NULL,
    name             VARCHAR(255) NOT NULL,
    phone_number     VARCHAR(50)  NOT NULL,
    profile_img_url  TEXT,
    user_type        user_type    NOT NULL,
    status           user_status  NOT NULL,
    created_at       TIMESTAMP WITHOUT TIME ZONE,
    updated_at       TIMESTAMP WITHOUT TIME ZONE,

    CONSTRAINT uk_users_supabase_user_id UNIQUE (supabase_user_id)
);

CREATE INDEX idx_users_user_type ON users (user_type);
CREATE INDEX idx_users_status ON users (status);
CREATE INDEX idx_users_supabase_user_id ON users (supabase_user_id);