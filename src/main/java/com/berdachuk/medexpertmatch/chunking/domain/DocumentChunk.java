package com.berdachuk.medexpertmatch.chunking.domain;

public record DocumentChunk(
        String id,
        String documentId,
        int chunkIndex,
        String chunkText,
        float[] embedding
) {}
