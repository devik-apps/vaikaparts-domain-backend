CREATE TABLE managers
(
    id           VARCHAR(255) PRIMARY KEY,
    manager_role manager_role NOT NULL,

    CONSTRAINT fk_managers_users FOREIGN KEY (id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_managers_role ON managers (manager_role);