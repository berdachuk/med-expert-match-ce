-- Flyway V2: Session tokens and audit log for v1.0 release
-- Non-breaking additions to V1 schema

CREATE TABLE IF NOT EXISTS api_session_token (
    id CHAR(24) PRIMARY KEY CHECK (id ~ '^[0-9a-fA-F]{24}$'),
    api_key VARCHAR(64) NOT NULL UNIQUE,
    description VARCHAR(255),
    rate_limit_tier VARCHAR(20) NOT NULL DEFAULT 'default' CHECK (rate_limit_tier IN ('default', 'high', 'unlimited')),
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_log (
    id CHAR(24) PRIMARY KEY CHECK (id ~ '^[0-9a-fA-F]{24}$'),
    action VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id VARCHAR(255),
    actor VARCHAR(255),
    details JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_log_action ON audit_log (action);
CREATE INDEX IF NOT EXISTS idx_audit_log_resource_type ON audit_log (resource_type);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON audit_log (created_at);
