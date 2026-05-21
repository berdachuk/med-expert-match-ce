package com.berdachuk.medexpertmatch.chunking.repository;

import com.berdachuk.medexpertmatch.chunking.domain.DocumentChunk;

import java.util.List;

public interface ChunkRepository {

    List<DocumentChunk> findByDocumentId(String documentId);

    DocumentChunk insert(DocumentChunk chunk);

    List<String> insertBatch(List<DocumentChunk> chunks);

    int deleteByDocumentId(String documentId);

    int deleteAll();

    void updateEmbedding(String chunkId, float[] embedding);

    void updateEmbeddings(List<DocumentChunk> chunks);
}
