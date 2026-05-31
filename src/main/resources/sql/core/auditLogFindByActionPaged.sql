SELECT id, action, resource_type, resource_id, actor, details, created_at
FROM audit_log
WHERE action = :action
ORDER BY created_at DESC
LIMIT :limit OFFSET :offset
