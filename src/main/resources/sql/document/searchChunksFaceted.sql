SELECT dc.id, dc.document_id, dc.chunk_index, dc.chunk_text,
       sd.title, sd.category, sd.source_name, sd.created_at,
       1 - (dc.embedding <=> :queryEmbedding::vector) AS similarity
FROM medexpertmatch.document_chunk dc
JOIN medexpertmatch.source_document sd ON dc.document_id = sd.id
WHERE dc.embedding IS NOT NULL
  AND (CAST(:category AS varchar) IS NULL OR sd.category = :category)
  AND (CAST(:source AS varchar) IS NULL OR sd.source_name = :source)
  AND (CAST(:fromDate AS timestamp) IS NULL OR sd.created_at >= CAST(:fromDate AS timestamp))
  AND (CAST(:toDate AS timestamp) IS NULL OR sd.created_at <= CAST(:toDate AS timestamp))
ORDER BY dc.embedding <=> :queryEmbedding::vector
LIMIT :limit
