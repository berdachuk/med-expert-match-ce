package com.berdachuk.medexpertmatch.llm.service;

import com.berdachuk.medexpertmatch.llm.domain.MatchJobStatus;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for async match jobs (match doctors, match-from-text).
 */
@Service
public class MatchJobStore {

    private final Map<String, MatchJobStatus> jobs = new ConcurrentHashMap<>();
    private static final int MAX_JOBS = 100;

    public String createJob() {
        String jobId = "match-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        jobs.put(jobId, MatchJobStatus.pending(jobId));
        evictOldJobs();
        return jobId;
    }

    public void completeJob(String jobId, MedicalAgentService.AgentResponse result) {
        jobs.put(jobId, MatchJobStatus.completed(jobId, result));
    }

    public void failJob(String jobId, String errorMessage) {
        jobs.put(jobId, MatchJobStatus.failed(jobId, errorMessage));
    }

    public MatchJobStatus getStatus(String jobId) {
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
