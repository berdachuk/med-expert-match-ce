package com.berdachuk.medexpertmatch.documents.domain;

public record SourceDocumentEntity(
        String id,
        String externalId,
        String title,
        String category,
        String sourceName,
        String sourceUrl,
        String content,
        String contentHash,
        String sourceFormat
) {}
