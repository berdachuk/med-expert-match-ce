UPDATE api_session_token
SET last_used_at = :lastUsedAt
WHERE id = :id
