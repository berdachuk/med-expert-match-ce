INSERT INTO audit_log (id, action, resource_type, resource_id, actor, details, created_at)
VALUES (:id, :action, :resourceType, :resourceId, :actor, CAST(:details AS jsonb), :createdAt)
