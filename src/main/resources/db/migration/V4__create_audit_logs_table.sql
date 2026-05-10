CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    file_id BIGINT,
    file_uuid VARCHAR(36),
    actor_id BIGINT NOT NULL,
    actor_role VARCHAR(10) NOT NULL,
    action VARCHAR(40) NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    acted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_actor_role CHECK (actor_role IN ('ROLE_USER', 'ROLE_ADMIN')),
    CONSTRAINT chk_audit_action CHECK (
        action IN (
            'FILE_UPLOADED',
            'FILE_DOWNLOADED',
            'FILE_DELETED',
            'FILE_REPLACED',
            'FILE_VIEWED',
            'PROFILE_UPDATED',
            'AVATAR_UPDATED',
            'CV_UPLOADED',
            'CV_REPLACED',
            'COVER_LETTER_UPLOADED',
            'COVER_LETTER_REPLACED',
            'SUMMARY_UPDATED',
            'ACCOUNT_CREATED',
            'ACCOUNT_DELETED'
        )
    )
);

CREATE INDEX idx_audit_user_id ON audit_logs (user_id);
CREATE INDEX idx_audit_file_id ON audit_logs (file_id);
CREATE INDEX idx_audit_action ON audit_logs (action);
CREATE INDEX idx_audit_acted_at ON audit_logs (acted_at);
CREATE INDEX idx_audit_actor_id ON audit_logs (actor_id);
CREATE INDEX idx_audit_user_action ON audit_logs (user_id, action);
