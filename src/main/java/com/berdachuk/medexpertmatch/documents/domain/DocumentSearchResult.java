package com.berdachuk.medexpertmatch.documents.domain;

public record DocumentSearchResult(
        String chunkId,
        String documentId,
        int chunkIndex,
        String chunkText,
        String documentTitle,
        String category,
        String sourceName,
        double similarity
) {}
