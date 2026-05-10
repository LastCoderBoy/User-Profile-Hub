CREATE TABLE file_metadata (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    file_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    stored_name VARCHAR(50) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    thumbnail_path VARCHAR(500),
    mime_type VARCHAR(100) NOT NULL,
    extension VARCHAR(10) NOT NULL,
    size_bytes BIGINT NOT NULL,
    checksum_sha256 VARCHAR(64) NOT NULL,
    scan_result VARCHAR(255),
    scanned_at TIMESTAMP,
    uploaded_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP,
    uploaded_by BIGINT NOT NULL,
    CONSTRAINT uk_file_uuid UNIQUE (uuid),
    CONSTRAINT uk_stored_name UNIQUE (stored_name),
    CONSTRAINT fk_file_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_file_type CHECK (file_type IN ('AVATAR', 'CV', 'COVER_LETTER')),
    CONSTRAINT chk_file_status CHECK (status IN ('PROCESSING', 'READY', 'INFECTED', 'DELETED'))
);

CREATE INDEX idx_file_uuid ON file_metadata (uuid);
CREATE INDEX idx_file_user_id ON file_metadata (user_id);
CREATE INDEX idx_file_type ON file_metadata (file_type);
CREATE INDEX idx_file_status ON file_metadata (status);
CREATE INDEX idx_file_user_type ON file_metadata (user_id, file_type);
CREATE INDEX idx_file_uploaded_at ON file_metadata (uploaded_at);
CREATE INDEX idx_file_deleted_at ON file_metadata (deleted_at);
