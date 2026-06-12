SELECT dc.id, dc.document_id, dc.chunk_index, dc.chunk_text, dc.embedding, dc.created_at
FROM medexpertmatch.document_chunk dc
WHERE dc.embedding IS NULL
ORDER BY dc.created_at
LIMIT :limit