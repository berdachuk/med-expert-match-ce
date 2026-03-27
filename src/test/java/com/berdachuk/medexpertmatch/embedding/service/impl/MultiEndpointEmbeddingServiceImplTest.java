package com.berdachuk.medexpertmatch.embedding.service.impl;

import com.berdachuk.medexpertmatch.core.util.LlmCallLimiter;
import com.berdachuk.medexpertmatch.embedding.multiendpoint.EmbeddingEndpointPool;
import com.berdachuk.medexpertmatch.medicalcase.service.MedicalCaseDescriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MultiEndpointEmbeddingServiceImplTest {

    private static final float[] VECTOR_5 = {0.1f, 0.2f, 0.3f, 0.4f, 0.5f};
    private static final float[] VECTOR_3 = {0.1f, 0.2f, 0.3f};
    private static final float[] VECTOR_4 = {0.1f, 0.2f, 0.3f, 0.4f};

    @Mock
    private EmbeddingEndpointPool pool;

    @Mock
    private MedicalCaseDescriptionService descriptionService;

    @Mock
    private LlmCallLimiter llmCallLimiter;

    private MultiEndpointEmbeddingServiceImpl embeddingService;

    @BeforeEach
    void setUp() {
        embeddingService = new MultiEndpointEmbeddingServiceImpl(pool, descriptionService, llmCallLimiter);
    }

    @Test
    void generateEmbedding_delegatesToPool() {
        List<Double> expected = List.of(
                (double) VECTOR_5[0],
                (double) VECTOR_5[1],
                (double) VECTOR_5[2],
                (double) VECTOR_5[3],
                (double) VECTOR_5[4]);

        when(pool.embed(anyString()))
                .thenReturn(CompletableFuture.completedFuture(expected));

        List<Double> result = embeddingService.generateEmbedding("test text");

        assertNotNull(result);
        assertEquals(5, result.size());
        assertEquals(0.1, result.get(0), 0.001);
    }

    @Test
    void generateEmbeddings_delegatesToPool() {
        List<Double> l1 = List.of((double) VECTOR_3[0], (double) VECTOR_3[1], (double) VECTOR_3[2]);
        List<Double> l2 = List.of((double) VECTOR_4[0], (double) VECTOR_4[1], (double) VECTOR_4[2], (double) VECTOR_4[3]);

        when(pool.embedBatch(anyList()))
                .thenReturn(List.of(
                        CompletableFuture.completedFuture(l1),
                        CompletableFuture.completedFuture(l2)));

        List<List<Double>> results = embeddingService.generateEmbeddings(List.of("a", "b"));

        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals(3, results.get(0).size());
        assertEquals(4, results.get(1).size());
    }

    @Test
    void generateEmbeddings_emptyList_returnsEmpty() {
        List<List<Double>> results = embeddingService.generateEmbeddings(List.of());

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void generateEmbeddingAsFloatArray_delegatesToGenerateEmbedding() {
        List<Double> embedding = List.of(0.1, 0.2, 0.3);
        when(pool.embed(anyString()))
                .thenReturn(CompletableFuture.completedFuture(embedding));

        float[] result = embeddingService.generateEmbeddingAsFloatArray("test");

        assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals(0.1f, result[0], 0.001f);
        assertEquals(0.3f, result[2], 0.001f);
    }
}
