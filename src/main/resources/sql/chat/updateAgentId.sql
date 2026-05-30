UPDATE medexpertmatch.chat
SET agent_id = :agentId,
    updated_at = :updatedAt
WHERE id = :id
