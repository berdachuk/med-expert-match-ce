package com.berdachuk.medexpertmatch.documents.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "medexpertmatch.documents.backfill.enabled", havingValue = "true", matchIfMissing = true)
public class EmbeddingBackfillScheduler {

    private static final String BACKFILL_CRON = "${medexpertmatch.documents.backfill.cron:0 0 2 * * *}";

    private final DocumentEmbeddingPipeline documentEmbeddingPipeline;

    public EmbeddingBackfillScheduler(DocumentEmbeddingPipeline documentEmbeddingPipeline) {
        this.documentEmbeddingPipeline = documentEmbeddingPipeline;
    }

    @Scheduled(cron = BACKFILL_CRON)
    public void backfillNullEmbeddings() {
        log.info("Starting scheduled backfill of chunks with NULL embeddings");
        try {
            documentEmbeddingPipeline.backfillNullEmbeddings();
        } catch (Exception e) {
            log.error("Scheduled embedding backfill failed", e);
        }
    }
}