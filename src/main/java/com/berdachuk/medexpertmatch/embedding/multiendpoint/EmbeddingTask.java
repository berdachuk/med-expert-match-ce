package com.berdachuk.medexpertmatch.embedding.multiendpoint;

import lombok.Getter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Embedding task submitted to the endpoint pool.
 * Supports both single-text and batch mode (multiple texts per API call).
 */
@Getter
class EmbeddingTask {
    private final List<String> texts;
    private final List<CompletableFuture<List<Double>>> futures;

    EmbeddingTask(String text) {
        this.texts = List.of(text);
        CompletableFuture<List<Double>> f = new CompletableFuture<>();
        this.futures = List.of(f);
    }

    EmbeddingTask(List<String> texts, List<CompletableFuture<List<Double>>> futures) {
        if (texts.size() != futures.size()) {
            throw new IllegalArgumentException("texts and futures must have same size");
        }
        this.texts = List.copyOf(texts);
        this.futures = List.copyOf(futures);
    }
}
