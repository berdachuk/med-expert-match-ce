package com.berdachuk.medexpertmatch.ingestion.service.impl;

import com.berdachuk.medexpertmatch.core.util.LlmCallLimiter;
import com.berdachuk.medexpertmatch.core.util.LlmClientType;
import com.berdachuk.medexpertmatch.embedding.service.EmbeddingService;
import com.berdachuk.medexpertmatch.ingestion.service.EmbeddingGeneratorService;
import com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataGenerationProgress;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Timer.Sample;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service implementation for generating embeddings for medical cases.
 * Uses batch processing with parallel execution for improved performance.
 */
@Slf4j
@Service
public class EmbeddingGeneratorServiceImpl implements EmbeddingGeneratorService {

    private final MedicalCaseRepository medicalCaseRepository;
    private final EmbeddingService embeddingService;
    private final LlmCallLimiter llmCallLimiter;
    private final MeterRegistry meterRegistry;
    private final Counter embeddingsGeneratedCounter;
    private final Timer embeddingsDurationTimer;

    @Value("${medexpertmatch.synthetic-data.embedding.thread-pool-size:10}")
    private int embeddingThreadPoolSize;

    @Value("${medexpertmatch.synthetic-data.embedding.batch-size:50}")
    private int embeddingBatchSize;

    @Value("${medexpertmatch.synthetic-data.progress-update-interval:100}")
    private int progressUpdateInterval;

    public EmbeddingGeneratorServiceImpl(
            MedicalCaseRepository medicalCaseRepository,
            EmbeddingService embeddingService,
            LlmCallLimiter llmCallLimiter,
            MeterRegistry meterRegistry) {
        this.medicalCaseRepository = medicalCaseRepository;
        this.embeddingService = embeddingService;
        this.llmCallLimiter = llmCallLimiter;
        this.meterRegistry = meterRegistry;

        this.embeddingsGeneratedCounter = Counter.builder("synthetic.data.embeddings.generated")
                .description("Total number of embeddings generated")
                .register(meterRegistry);
        this.embeddingsDurationTimer = Timer.builder("synthetic.data.embeddings.duration")
                .description("Time taken to generate embeddings")
                .register(meterRegistry);
    }

    @Override
    public void generateEmbeddings(SyntheticDataGenerationProgress progress) {
        List<MedicalCase> cases = medicalCaseRepository.findWithoutEmbeddings();

        int totalRecords = cases.size();
        Sample sample = Timer.start(meterRegistry);
        long startTime = System.currentTimeMillis();
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        log.info("Starting batch embedding generation for {} medical cases (batch size: {}, thread pool size: {})",
                totalRecords, embeddingBatchSize, embeddingThreadPoolSize);

        if (totalRecords == 0) {
            log.info("No cases without embeddings found");
            return;
        }

        List<List<MedicalCase>> batches = new ArrayList<>();
        for (int i = 0; i < cases.size(); i += embeddingBatchSize) {
            int end = Math.min(i + embeddingBatchSize, cases.size());
            batches.add(cases.subList(i, end));
        }

        int chatMaxConcurrentCalls = llmCallLimiter.getMaxConcurrentCalls(LlmClientType.CHAT);

        if (chatMaxConcurrentCalls == 1) {
            log.info("CHAT max concurrent calls is 1 - processing batches sequentially");
            for (List<MedicalCase> batch : batches) {
                if (progress != null && progress.isCancelled()) {
                    log.info("Generation cancelled before processing all batches");
                    break;
                }

                processBatchEmbeddings(batch, processedCount, successCount, failedCount,
                        totalRecords, startTime, progress);
            }
        } else {
            log.info("Split {} cases into {} batches for parallel batch processing", totalRecords, batches.size());

            ExecutorService executor = Executors.newFixedThreadPool(embeddingThreadPoolSize);
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            try {
                for (List<MedicalCase> batch : batches) {
                    if (progress != null && progress.isCancelled()) {
                        log.info("Generation cancelled before processing all batches");
                        break;
                    }

                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        if (progress != null && progress.isCancelled()) {
                            return;
                        }

                        processBatchEmbeddings(batch, processedCount, successCount, failedCount,
                                totalRecords, startTime, progress);
                    }, executor);

                    futures.add(future);
                }

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            } finally {
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }

        long endTime = System.currentTimeMillis();
        long totalElapsedTime = endTime - startTime;
        double totalItemsPerSecond = (processedCount.get() * 1000.0) / totalElapsedTime;

        sample.stop(embeddingsDurationTimer);

        log.info(String.format("Batch embedding generation completed. Total: %d, Success: %d, Failed: %d, " +
                        "Total time: %.3fs, Overall rate: %.2f items/sec, Batches processed: %d",
                processedCount.get(), successCount.get(), failedCount.get(),
                totalElapsedTime / 1000.0, totalItemsPerSecond, batches.size()));
    }

    private void processBatchEmbeddings(List<MedicalCase> batch, AtomicInteger processedCount,
                                        AtomicInteger successCount, AtomicInteger failedCount,
                                        int totalRecords, long startTime,
                                        SyntheticDataGenerationProgress progress) {
        if (batch.isEmpty()) {
            return;
        }

        try {
            long embeddingStartTime = System.currentTimeMillis();
            List<List<Double>> embeddings = embeddingService.generateEmbeddingsForMedicalCases(batch);
            long embeddingEndTime = System.currentTimeMillis();

            int batchSuccessCount = 0;
            for (int i = 0; i < batch.size() && i < embeddings.size(); i++) {
                List<Double> embedding = embeddings.get(i);
                if (embedding != null && !embedding.isEmpty()) {
                    MedicalCase medicalCase = batch.get(i);
                    int originalDimension = embedding.size();
                    medicalCaseRepository.updateEmbedding(medicalCase.id(), embedding, originalDimension);
                    batchSuccessCount++;
                    embeddingsGeneratedCounter.increment();
                }
            }

            int currentProcessed = processedCount.addAndGet(batch.size());
            successCount.addAndGet(batchSuccessCount);
            int batchFailed = batch.size() - batchSuccessCount;
            if (batchFailed > 0) {
                failedCount.addAndGet(batchFailed);
            }

            if (currentProcessed % progressUpdateInterval == 0 || currentProcessed == totalRecords) {
                long currentTime = System.currentTimeMillis();
                long elapsedTime = currentTime - startTime;
                double itemsPerSecond = (currentProcessed * 1000.0) / elapsedTime;
                double batchTime = (embeddingEndTime - embeddingStartTime) / 1000.0;
                double avgTimePerCase = batchTime / batch.size();

                int progressPercent = totalRecords > 0 ? (currentProcessed * 100 / totalRecords) : 0;

                if (progress != null && totalRecords > 0) {
                    int embeddingProgress = 70 + (progressPercent * 20 / 100);
                    progress.updateProgress(embeddingProgress, "Embeddings",
                            String.format("Generating embeddings: %d/%d (%d%%)", currentProcessed, totalRecords, progressPercent));
                }

                log.info(String.format("Progress: %d/%d records processed (%d%% complete), Success: %d, Failed: %d, " +
                                "Items/sec: %.2f, Batch time: %.3fs (%.3fs per case)",
                        currentProcessed, totalRecords, progressPercent,
                        successCount.get(), failedCount.get(),
                        itemsPerSecond, batchTime, avgTimePerCase));
            }
        } catch (Exception e) {
            failedCount.addAndGet(batch.size());
            log.error("Error during batch embedding generation for {} cases", batch.size(), e);
        }
    }
}
