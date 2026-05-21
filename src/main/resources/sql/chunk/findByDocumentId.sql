SELECT id, document_id, chunk_index, chunk_text
FROM medexpertmatch.document_chunk WHERE document_id = :documentId ORDER BY chunk_index
