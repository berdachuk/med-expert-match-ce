package com.berdachuk.medexpertmatch.embedding.multiendpoint;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pool of embedding endpoints with worker-per-endpoint architecture.
 * Each endpoint has dedicated worker threads that pull tasks from a shared queue.
 * Failed endpoints are automatically skipped for a configurable duration.
 * <p>
 * When {@code apiBatchSize > 1}, uses OpenAI-compatible batch API: multiple texts per HTTP call,
 * reducing round-trips and improving throughput (2-5x faster).
 */
@Slf4j
public class EmbeddingEndpointPool {

    private static final int STATS_LOG_INTERVAL = 100;

    private final List<EndpointState> endpoints;
    private final BlockingQueue<EmbeddingTask> taskQueue;
    private final long skipDurationMs;
    private final int apiBatchSize;
    private final ExecutorService executor;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final AtomicInteger totalCompleted = new AtomicInteger(0);

    public EmbeddingEndpointPool(List<EndpointState> endpoints, int workerPerEndpoint, int skipDurationMin) {
        this(endpoints, workerPerEndpoint, skipDurationMin, 1);
    }

    public EmbeddingEndpointPool(List<EndpointState> endpoints, int workerPerEndpoint, int skipDurationMin,
            int apiBatchSize) {
        this(endpoints,
                endpoints.stream().map(e -> workerPerEndpoint).toList(),
                skipDurationMin,
                apiBatchSize);
    }

    public EmbeddingEndpointPool(List<EndpointState> endpoints, List<Integer> workersPerEndpoint,
            int skipDurationMin, int apiBatchSize) {
        this.endpoints = List.copyOf(endpoints);
        this.taskQueue = new LinkedBlockingQueue<>();
        this.skipDurationMs = TimeUnit.MINUTES.toMillis(skipDurationMin);
        this.apiBatchSize = Math.max(1, apiBatchSize);

        int totalWorkers = 0;
        for (int i = 0; i < endpoints.size(); i++) {
            int w = i < workersPerEndpoint.size() ? Math.max(1, workersPerEndpoint.get(i)) : 1;
            totalWorkers += w;
        }
        int totalWorkersFinal = totalWorkers;
        this.executor = Executors.newFixedThreadPool(totalWorkers, r -> {
            Thread t = new Thread(r, "embedding-pool-worker");
            t.setDaemon(false);
            return t;
        });

        for (int i = 0; i < endpoints.size(); i++) {
            EndpointState endpoint = endpoints.get(i);
            int workers = i < workersPerEndpoint.size() ? Math.max(1, workersPerEndpoint.get(i)) : 1;
            for (int w = 0; w < workers; w++) {
                executor.submit(() -> runWorker(endpoint));
            }
        }

        log.info("EmbeddingEndpointPool started with {} endpoints, {} workers total, api-batch-size={}",
                endpoints.size(), totalWorkersFinal, this.apiBatchSize);
    }

    private void runWorker(EndpointState endpoint) {
        while (!shutdown.get()) {
            try {
                EmbeddingTask task = taskQueue.poll(1, TimeUnit.SECONDS);
                if (task == null) {
                    continue;
                }

                if (endpoint.isSkipped()) {
                    long elapsed = System.currentTimeMillis() - endpoint.getLastFailureTime();
                    if (elapsed >= skipDurationMs) {
                        endpoint.setSkipped(false);
                        log.debug("Endpoint {} recovered from skip, retrying", endpoint.getUrl());
                    } else {
                        taskQueue.offer(task);
                        Thread.sleep(Math.min(5000, skipDurationMs - elapsed));
                        continue;
                    }
                }

                try {
                    processTask(endpoint, task);
                } catch (Exception e) {
                    log.warn("Endpoint {} failed for batch (size={}): {}",
                            endpoint.getUrl(), task.getTexts().size(), e.getMessage());
                    endpoint.setLastFailureTime(System.currentTimeMillis());
                    endpoint.setSkipped(true);
                    taskQueue.offer(task);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void processTask(EndpointState endpoint, EmbeddingTask task) {
        List<String> texts = task.getTexts();
        List<CompletableFuture<List<Double>>> futures = task.getFutures();

        List<List<Double>> results = embedBatchWithModel(endpoint.getEmbeddingModel(), texts);

        for (int i = 0; i < futures.size(); i++) {
            List<Double> embedding = i < results.size() ? results.get(i) : List.of();
            futures.get(i).complete(embedding);
        }

        int completed = texts.size();
        endpoint.getCompletedCount().addAndGet(completed);
        int n = totalCompleted.addAndGet(completed);
        if (n % STATS_LOG_INTERVAL == 0) {
            String stats = endpoints.stream()
                    .map(e -> e.getUrl() + "=" + e.getCompletedCount().get())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            log.info("EmbeddingEndpointPool: {} tasks completed, per-endpoint: [{}]", n, stats);
        }
    }

    private static List<List<Double>> embedBatchWithModel(EmbeddingModel model, List<String> texts) {
        if (texts.isEmpty()) {
            return List.of();
        }
        EmbeddingResponse response = model.embedForResponse(texts);
        List<List<Double>> results = new ArrayList<>(response.getResults().size());
        for (var embeddingResult : response.getResults()) {
            float[] output = embeddingResult.getOutput();
            List<Double> vec = new ArrayList<>(output.length);
            for (float v : output) {
                vec.add((double) v);
            }
            results.add(vec);
        }
        return results;
    }

    /**
     * Submits a single text for embedding.
     *
     * @param text text to embed
     * @return future that completes with the embedding vector
     */
    public CompletableFuture<List<Double>> embed(String text) {
        EmbeddingTask task = new EmbeddingTask(text);
        taskQueue.offer(task);
        return task.getFutures().get(0);
    }

    /**
     * Submits multiple texts for embedding in parallel.
     * When apiBatchSize &gt; 1, groups texts into sub-batches for fewer API calls (2-5x faster).
     *
     * @param texts texts to embed
     * @return list of futures, one per text (order preserved)
     */
    public List<CompletableFuture<List<Double>>> embedBatch(List<String> texts) {
        if (texts.isEmpty()) {
            return List.of();
        }

        if (apiBatchSize <= 1) {
            List<CompletableFuture<List<Double>>> futures = new ArrayList<>(texts.size());
            for (String text : texts) {
                EmbeddingTask task = new EmbeddingTask(text);
                taskQueue.offer(task);
                futures.add(task.getFutures().get(0));
            }
            return futures;
        }

        List<CompletableFuture<List<Double>>> allFutures = new ArrayList<>(texts.size());
        for (int i = 0; i < texts.size(); i += apiBatchSize) {
            int end = Math.min(i + apiBatchSize, texts.size());
            List<String> subBatch = texts.subList(i, end);
            List<CompletableFuture<List<Double>>> subFutures = new ArrayList<>(subBatch.size());
            for (int j = 0; j < subBatch.size(); j++) {
                subFutures.add(new CompletableFuture<>());
            }
            EmbeddingTask task = new EmbeddingTask(subBatch, subFutures);
            taskQueue.offer(task);
            allFutures.addAll(subFutures);
        }
        return allFutures;
    }

    @PreDestroy
    public void shutdown() {
        if (shutdown.compareAndSet(false, true)) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("EmbeddingEndpointPool shutdown complete");
        }
    }
}
