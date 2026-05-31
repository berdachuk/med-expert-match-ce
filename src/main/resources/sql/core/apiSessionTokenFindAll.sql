SELECT id, api_key, description, rate_limit_tier, expires_at, created_at, last_used_at
FROM api_session_token
ORDER BY created_at DESC
