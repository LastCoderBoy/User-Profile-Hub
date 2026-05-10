CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    user_id BIGINT NOT NULL,
    CONSTRAINT uk_token UNIQUE (token),
    CONSTRAINT fk_refresh_token_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_token ON refresh_tokens (token);
CREATE INDEX idx_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_expires_at ON refresh_tokens (expires_at);
