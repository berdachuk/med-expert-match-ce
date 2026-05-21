package com.berdachuk.medexpertmatch.documents.service.impl;

import com.berdachuk.medexpertmatch.chunking.domain.DocumentChunk;
import com.berdachuk.medexpertmatch.chunking.repository.ChunkRepository;
import com.berdachuk.medexpertmatch.embedding.service.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(name = "medexpertmatch.documents.enabled", havingValue = "true")
public class DocumentEmbeddingPipeline {

    private final EmbeddingService embeddingService;
    private final ChunkRepository chunkRepository;

    public DocumentEmbeddingPipeline(EmbeddingService embeddingService, ChunkRepository chunkRepository) {
        this.embeddingService = embeddingService;
        this.chunkRepository = chunkRepository;
    }

    public void embedChunks(List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        List<String> texts = chunks.stream().map(DocumentChunk::chunkText).toList();

        try {
            List<List<Double>> embeddings = embeddingService.generateEmbeddings(texts);

            List<DocumentChunk> embeddedChunks = new ArrayList<>();
            for (int i = 0; i < chunks.size() && i < embeddings.size(); i++) {
                List<Double> embedding = embeddings.get(i);
                float[] embeddingArray = new float[embedding.size()];
                for (int j = 0; j < embedding.size(); j++) {
                    embeddingArray[j] = embedding.get(j).floatValue();
                }

                DocumentChunk chunk = chunks.get(i);
                DocumentChunk embeddedChunk = new DocumentChunk(
                        chunk.id(), chunk.documentId(), chunk.chunkIndex(),
                        chunk.chunkText(), embeddingArray);
                embeddedChunks.add(embeddedChunk);
            }

            if (!embeddedChunks.isEmpty()) {
                chunkRepository.updateEmbeddings(embeddedChunks);
                log.debug("Embedded {} chunks", embeddedChunks.size());
            }
        } catch (Exception e) {
            log.warn("Failed to embed chunks: {}", e.getMessage());
        }
    }

    public void embedChunk(DocumentChunk chunk) {
        try {
            float[] embedding = embeddingService.generateEmbeddingAsFloatArray(chunk.chunkText());
            DocumentChunk embeddedChunk = new DocumentChunk(
                    chunk.id(), chunk.documentId(), chunk.chunkIndex(),
                    chunk.chunkText(), embedding);
            chunkRepository.updateEmbedding(embeddedChunk.id(), embedding);
            log.debug("Embedded chunk {}", chunk.id());
        } catch (Exception e) {
            log.warn("Failed to embed chunk {}: {}", chunk.id(), e.getMessage());
        }
    }
}
