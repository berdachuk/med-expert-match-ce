SELECT dc.id, dc.document_id, dc.chunk_index, dc.chunk_text,
       sd.title, sd.category, sd.source_name,
       1 - (dc.embedding <=> :queryEmbedding::vector) AS similarity
FROM medexpertmatch.document_chunk dc
JOIN medexpertmatch.source_document sd ON dc.document_id = sd.id
WHERE dc.embedding IS NOT NULL
ORDER BY dc.embedding <=> :queryEmbedding::vector
LIMIT :limit
