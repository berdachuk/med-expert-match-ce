SELECT id, user_id, name, agent_id, is_default, created_at, updated_at, last_activity_at, message_count
FROM medexpertmatch.chat
WHERE user_id = :userId AND is_default = TRUE
LIMIT 1
