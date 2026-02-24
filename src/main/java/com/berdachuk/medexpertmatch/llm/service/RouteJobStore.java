package com.berdachuk.medexpertmatch.llm.service;

import com.berdachuk.medexpertmatch.llm.domain.RouteJobStatus;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for async route-case jobs.
 */
@Service
public class RouteJobStore {

    private final Map<String, RouteJobStatus> jobs = new ConcurrentHashMap<>();
    private static final int MAX_JOBS = 100;

    public String createJob() {
        String jobId = "route-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        jobs.put(jobId, RouteJobStatus.pending(jobId));
        evictOldJobs();
        return jobId;
    }

    public void completeJob(String jobId, MedicalAgentService.AgentResponse result) {
        jobs.put(jobId, RouteJobStatus.completed(jobId, result));
    }

    public void failJob(String jobId, String errorMessage) {
        jobs.put(jobId, RouteJobStatus.failed(jobId, errorMessage));
    }

    public RouteJobStatus getStatus(String jobId) {
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
