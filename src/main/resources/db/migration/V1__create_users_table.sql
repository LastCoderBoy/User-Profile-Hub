CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50),
    email VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(10) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    title VARCHAR(150),
    summary TEXT,
    phone_number VARCHAR(20),
    location VARCHAR(100),
    linkedin_url VARCHAR(255),
    website_url VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP,
    CONSTRAINT uk_user_uuid UNIQUE (uuid),
    CONSTRAINT uk_user_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('ROLE_USER', 'ROLE_ADMIN'))
);

CREATE INDEX idx_user_uuid ON users (uuid);
CREATE INDEX idx_user_email ON users (email);
CREATE INDEX idx_user_role ON users (role);
CREATE INDEX idx_user_created_at ON users (created_at);
CREATE INDEX idx_user_deleted_at ON users (deleted_at);
