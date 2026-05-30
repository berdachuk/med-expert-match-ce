-- Flyway V3: AI chat sessions and message history (M13)
-- Non-breaking additions; medexpertmatch schema

CREATE TABLE IF NOT EXISTS medexpertmatch.chat (
    id CHAR(24) PRIMARY KEY CHECK (id ~ '^[0-9a-fA-F]{24}$'),
    user_id VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    agent_id VARCHAR(50) NOT NULL DEFAULT 'auto',
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    message_count INT DEFAULT 0,
    metadata JSONB
);

CREATE UNIQUE INDEX IF NOT EXISTS chat_user_default_unique
    ON medexpertmatch.chat (user_id, is_default) WHERE is_default = TRUE;
CREATE INDEX IF NOT EXISTS chat_user_id_idx ON medexpertmatch.chat (user_id);
CREATE INDEX IF NOT EXISTS chat_last_activity_at_idx ON medexpertmatch.chat (last_activity_at DESC);

CREATE TABLE IF NOT EXISTS medexpertmatch.chat_message (
    id CHAR(24) PRIMARY KEY CHECK (id ~ '^[0-9a-fA-F]{24}$'),
    chat_id CHAR(24) NOT NULL REFERENCES medexpertmatch.chat (id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('user', 'assistant', 'system')),
    content TEXT NOT NULL,
    sequence_number INT NOT NULL,
    tokens_used INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB,
    CONSTRAINT chat_message_chat_sequence_unique UNIQUE (chat_id, sequence_number)
);

CREATE INDEX IF NOT EXISTS chat_message_chat_id_idx ON medexpertmatch.chat_message (chat_id);
CREATE INDEX IF NOT EXISTS chat_message_chat_id_sequence_idx ON medexpertmatch.chat_message (chat_id, sequence_number);
