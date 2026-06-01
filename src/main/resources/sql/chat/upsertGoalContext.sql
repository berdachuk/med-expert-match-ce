INSERT INTO medexpertmatch.chat_goal_context (session_id, goal_type, case_id, updated_at)
VALUES (:sessionId, :goalType, :caseId, :updatedAt)
ON CONFLICT (session_id) DO UPDATE SET
    goal_type = EXCLUDED.goal_type,
    case_id = EXCLUDED.case_id,
    updated_at = EXCLUDED.updated_at
