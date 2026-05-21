SELECT id, external_id, title, category, source_name, source_url, content, content_hash, source_format
FROM medexpertmatch.source_document WHERE content_hash = :contentHash
