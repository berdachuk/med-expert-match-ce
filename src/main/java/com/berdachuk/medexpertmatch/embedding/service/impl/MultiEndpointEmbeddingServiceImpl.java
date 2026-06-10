package com.berdachuk.medexpertmatch.embedding.service.impl;

import com.berdachuk.medexpertmatch.core.util.LlmCallLimiter;
import com.berdachuk.medexpertmatch.embedding.exception.EmbeddingException;
import com.berdachuk.medexpertmatch.embedding.multiendpoint.EmbeddingEndpointPool;
import com.berdachuk.medexpertmatch.embedding.service.EmbeddingService;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.service.MedicalCaseDescriptionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * EmbeddingService that delegates text embedding to a multi-endpoint pool.
 * Active when {@link EmbeddingEndpointPool} is configured.
 */
@Service
@Primary
@ConditionalOnBean(EmbeddingEndpointPool.class)
public class MultiEndpointEmbeddingServiceImpl implements EmbeddingService {

    private static final long EMBEDDING_TIMEOUT_MINUTES = 10;

    private final EmbeddingEndpointPool pool;
    private final MedicalCaseDescriptionService descriptionService;
    private final LlmCallLimiter llmCallLimiter;

    public MultiEndpointEmbeddingServiceImpl(
            EmbeddingEndpointPool pool,
            MedicalCaseDescriptionService descriptionService,
            LlmCallLimiter llmCallLimiter) {
        this.pool = pool;
        this.descriptionService = descriptionService;
        this.llmCallLimiter = llmCallLimiter;
    }

    @Override
    public List<Double> generateEmbedding(String text) {
        try {
            return pool.embed(text)
                    .get(EMBEDDING_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EmbeddingException("Embedding interrupted", e);
        } catch (ExecutionException e) {
            throw new EmbeddingException("Embedding failed", e.getCause());
        } catch (TimeoutException e) {
            throw new EmbeddingException("Embedding timed out after " + EMBEDDING_TIMEOUT_MINUTES + " minutes", e);
        }
    }

    @Override
    public List<List<Double>> generateEmbeddings(List<String> texts) {
        if (texts.isEmpty()) {
            return List.of();
        }

        List<CompletableFuture<List<Double>>> futures = pool.embedBatch(texts);
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(EMBEDDING_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EmbeddingException("Batch embedding interrupted", e);
        } catch (ExecutionException e) {
            throw new EmbeddingException("Batch embedding failed", e.getCause());
        } catch (TimeoutException e) {
            throw new EmbeddingException("Batch embedding timed out", e);
        }

        List<List<Double>> results = new ArrayList<>(texts.size());
        for (CompletableFuture<List<Double>> future : futures) {
            results.add(future.join());
        }
        return results;
    }

    @Override
    public float[] generateEmbeddingAsFloatArray(String text) {
        List<Double> embedding = generateEmbedding(text);
        float[] result = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            result[i] = embedding.get(i).floatValue();
        }
        return result;
    }

    @Override
    public List<Double> generateEmbeddingForMedicalCase(MedicalCase medicalCase) {
        return MedicalCaseEmbeddingSupport.embeddingForMedicalCase(medicalCase, descriptionService, this::generateEmbedding);
    }

    @Override
    public List<List<Double>> generateEmbeddingsForMedicalCases(List<MedicalCase> medicalCases) {
        return MedicalCaseEmbeddingSupport.embeddingsForMedicalCases(
                medicalCases, descriptionService, llmCallLimiter, this::generateEmbeddings);
    }
}
