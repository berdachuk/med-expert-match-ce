package com.berdachuk.medexpertmatch.llm.service;

import com.berdachuk.medexpertmatch.llm.domain.PrioritizeJobStatus;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for async queue prioritization jobs.
 */
@Service
public class PrioritizeJobStore {

    private final Map<String, PrioritizeJobStatus> jobs = new ConcurrentHashMap<>();
    private static final int MAX_JOBS = 100;
    private static final long JOB_TTL_MS = 30 * 60 * 1000;

    public String createJob() {
        String jobId = "prioritize-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        jobs.put(jobId, PrioritizeJobStatus.pending(jobId));
        evictOldJobs();
        return jobId;
    }

    public void completeJob(String jobId, com.berdachuk.medexpertmatch.llm.service.MedicalAgentService.AgentResponse result) {
        jobs.put(jobId, PrioritizeJobStatus.completed(jobId, result));
    }

    public void failJob(String jobId, String errorMessage) {
        jobs.put(jobId, PrioritizeJobStatus.failed(jobId, errorMessage));
    }

    public PrioritizeJobStatus getStatus(String jobId) {
        return jobs.get(jobId);
    }

    private void evictOldJobs() {
        if (jobs.size() > MAX_JOBS) {
            jobs.keySet().stream()
                    .limit(jobs.size() - MAX_JOBS)
                    .forEach(jobs::remove);
        }
    }
}
