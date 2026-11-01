-- Users of the app. Email is unique; the password is stored only as a hash.
CREATE TABLE users (
    id            UUID PRIMARY KEY,
    email         VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL
);

-- One account per email. The index also makes the unique check and login lookups fast.
CREATE UNIQUE INDEX ux_users_email ON users (email);
