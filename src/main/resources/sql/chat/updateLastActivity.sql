UPDATE medexpertmatch.chat
SET last_activity_at = :lastActivityAt, updated_at = :updatedAt, message_count = message_count + 1
WHERE id = :id
