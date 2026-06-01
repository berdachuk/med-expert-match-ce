SELECT session_id, goal_type, case_id, updated_at
FROM medexpertmatch.chat_goal_context
WHERE session_id = :sessionId
