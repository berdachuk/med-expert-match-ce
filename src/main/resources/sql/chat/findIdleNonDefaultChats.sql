SELECT id, user_id, name, agent_id, is_default, created_at, updated_at, last_activity_at, message_count
FROM medexpertmatch.chat
WHERE is_default = FALSE
  AND last_activity_at < :cutoff
ORDER BY last_activity_at ASC
LIMIT :limit
