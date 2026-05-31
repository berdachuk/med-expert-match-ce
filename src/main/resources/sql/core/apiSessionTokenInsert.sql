INSERT INTO api_session_token (id, api_key, description, rate_limit_tier, expires_at, created_at, last_used_at)
VALUES (:id, :apiKey, :description, :rateLimitTier, :expiresAt, :createdAt, :lastUsedAt)
