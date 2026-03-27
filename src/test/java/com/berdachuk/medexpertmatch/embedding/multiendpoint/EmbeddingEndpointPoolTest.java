package com.berdachuk.medexpertmatch.embedding.multiendpoint;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmbeddingEndpointPoolTest {

    private static final float[] VECTOR_5 = {0.1f, 0.2f, 0.3f, 0.4f, 0.5f};
    private static final float[] VECTOR_3 = {0.1f, 0.2f, 0.3f};
    private static final float[] VECTOR_4 = {0.1f, 0.2f, 0.3f, 0.4f};

    @Mock
    private EmbeddingModel embeddingModel;

    private EmbeddingEndpointPool pool;

    @AfterEach
    void tearDown() {
        if (pool != null) {
            pool.shutdown();
        }
    }

    @Test
    void embed_returnsResultFromEndpoint() throws ExecutionException, InterruptedException, TimeoutException {
        EmbeddingResponse response = new EmbeddingResponse(
                List.of(new Embedding(VECTOR_5, 0)));

        when(embeddingModel.embedForResponse(anyList())).thenReturn(response);

        EndpointState endpoint = new EndpointState("http://localhost:11434", "test-model", embeddingModel);
        pool = new EmbeddingEndpointPool(List.of(endpoint), 1, 10);

        CompletableFuture<List<Double>> future = pool.embed("test text");
        List<Double> result = future.get(5, TimeUnit.SECONDS);

        assertNotNull(result);
        assertEquals(5, result.size());
        assertEquals(0.1, result.get(0), 0.001);
    }

    @Test
    void embedBatch_submitsAllAndReturnsFutures() throws ExecutionException, InterruptedException, TimeoutException {
        when(embeddingModel.embedForResponse(anyList())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            List<String> texts = inv.getArgument(0);
            if (texts.size() == 1) {
                return new EmbeddingResponse(
                        List.of(new Embedding(texts.get(0).equals("a") ? VECTOR_3 : VECTOR_4, 0)));
            }
            return new EmbeddingResponse(List.of(
                    new Embedding(VECTOR_3, 0),
                    new Embedding(VECTOR_4, 1)));
        });

        EndpointState endpoint = new EndpointState("http://localhost:11434", "test-model", embeddingModel);
        pool = new EmbeddingEndpointPool(List.of(endpoint), 1, 10);

        List<CompletableFuture<List<Double>>> futures = pool.embedBatch(List.of("a", "b"));

        assertEquals(2, futures.size());
        List<Double> r1 = futures.get(0).get(5, TimeUnit.SECONDS);
        List<Double> r2 = futures.get(1).get(5, TimeUnit.SECONDS);

        assertEquals(3, r1.size());
        assertEquals(4, r2.size());
    }

    @Test
    void embedBatch_withApiBatchSize_batchesTextsIntoFewerApiCalls()
            throws ExecutionException, InterruptedException, TimeoutException {
        when(embeddingModel.embedForResponse(anyList())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            List<String> texts = inv.getArgument(0);
            return new EmbeddingResponse(List.of(
                    new Embedding(texts.get(0).equals("a") ? VECTOR_3 : VECTOR_4, 0),
                    new Embedding(texts.get(1).equals("b") ? VECTOR_4 : VECTOR_3, 1)));
        });

        EndpointState endpoint = new EndpointState("http://localhost:11434", "test-model", embeddingModel);
        pool = new EmbeddingEndpointPool(List.of(endpoint), 1, 10, 2);

        List<CompletableFuture<List<Double>>> futures = pool.embedBatch(List.of("a", "b"));

        assertEquals(2, futures.size());
        List<Double> r1 = futures.get(0).get(5, TimeUnit.SECONDS);
        List<Double> r2 = futures.get(1).get(5, TimeUnit.SECONDS);

        assertEquals(3, r1.size());
        assertEquals(4, r2.size());
    }
}
