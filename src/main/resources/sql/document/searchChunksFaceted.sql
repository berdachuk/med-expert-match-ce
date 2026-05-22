SELECT dc.id, dc.document_id, dc.chunk_index, dc.chunk_text,
       sd.title, sd.category, sd.source_name, sd.created_at,
       1 - (dc.embedding <=> :queryEmbedding::vector) AS similarity
FROM medexpertmatch.document_chunk dc
JOIN medexpertmatch.source_document sd ON dc.document_id = sd.id
WHERE dc.embedding IS NOT NULL
  AND (:category IS NULL OR sd.category = :category)
  AND (:source IS NULL OR sd.source_name = :source)
  AND (:fromDate IS NULL OR sd.created_at >= :fromDate::timestamp)
  AND (:toDate IS NULL OR sd.created_at <= :toDate::timestamp)
ORDER BY dc.embedding <=> :queryEmbedding::vector
LIMIT :limit
