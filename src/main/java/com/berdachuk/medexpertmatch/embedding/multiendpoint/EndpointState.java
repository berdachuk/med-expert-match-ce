package com.berdachuk.medexpertmatch.embedding.multiendpoint;

import lombok.Getter;
import lombok.Setter;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe state for a single embedding endpoint.
 */
@Getter
public class EndpointState {
    private final String url;
    private final String model;
    private final EmbeddingModel embeddingModel;
    private final AtomicInteger completedCount = new AtomicInteger(0);

    @Setter
    private volatile long lastFailureTime;
    @Setter
    private volatile boolean skipped;

    public EndpointState(String url, String model, EmbeddingModel embeddingModel) {
        this.url = url;
        this.model = model;
        this.embeddingModel = embeddingModel;
        this.lastFailureTime = 0;
        this.skipped = false;
    }
}
