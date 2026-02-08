package com.berdachuk.medexpertmatch.ingestion.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for tracking synthetic data generation progress.
 * Stores progress in memory (thread-safe).
 */
@Slf4j
@Service
public class SyntheticDataGenerationProgressService {

    private final Map<String, SyntheticDataGenerationProgress> progressMap = new ConcurrentHashMap<>();

    /**
     * Creates a new progress tracker for a job.
     *
     * @param jobId Unique job identifier
     * @return Progress tracker
     */
    public SyntheticDataGenerationProgress createProgress(String jobId) {
        SyntheticDataGenerationProgress progress = new SyntheticDataGenerationProgress(jobId);
        progressMap.put(jobId, progress);
        log.debug("Created progress tracker for job: {}", jobId);
        return progress;
    }

    /**
     * Gets progress for a job.
     *
     * @param jobId Job identifier
     * @return Progress tracker or null if not found
     */
    public SyntheticDataGenerationProgress getProgress(String jobId) {
        return progressMap.get(jobId);
    }

    /**
     * Removes progress tracker (cleanup after completion).
     *
     * @param jobId Job identifier
     */
    public void removeProgress(String jobId) {
        progressMap.remove(jobId);
        log.debug("Removed progress tracker for job: {}", jobId);
    }

    /**
     * Cancels a running job.
     *
     * @param jobId Job identifier
     * @return true if job was cancelled, false if not found or already completed
     */
    public boolean cancelJob(String jobId) {
        SyntheticDataGenerationProgress progress = progressMap.get(jobId);
        if (progress != null && "running".equals(progress.getStatus())) {
            progress.cancel();
            log.info("Cancelled job: {}", jobId);
            return true;
        }
        return false;
    }

    /**
     * Cleans up old completed jobs (older than 1 hour).
     */
    public void cleanupOldJobs() {
        progressMap.entrySet().removeIf(entry -> {
            SyntheticDataGenerationProgress progress = entry.getValue();
            if (progress.getEndTime() != null) {
                return progress.getEndTime().isBefore(
                        java.time.LocalDateTime.now().minusHours(1));
            }
            return false;
        });
    }
}
