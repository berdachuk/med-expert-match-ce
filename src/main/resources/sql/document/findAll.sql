SELECT id, external_id, title, category, source_name, source_url, content, content_hash, source_format
FROM medexpertmatch.source_document ORDER BY created_at DESC
LIMIT :limit
