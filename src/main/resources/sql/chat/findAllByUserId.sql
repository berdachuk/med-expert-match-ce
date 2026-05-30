SELECT id, user_id, name, agent_id, is_default, created_at, updated_at, last_activity_at, message_count
FROM medexpertmatch.chat
WHERE user_id = :userId
ORDER BY last_activity_at DESC
