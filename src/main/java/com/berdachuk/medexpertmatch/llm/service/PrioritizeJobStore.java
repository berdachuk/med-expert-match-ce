package com.berdachuk.medexpertmatch.llm.service;

import com.berdachuk.medexpertmatch.core.service.JobStatusWebSocketPublisher;
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
    private final JobStatusWebSocketPublisher jobStatusWebSocketPublisher;
    private static final int MAX_JOBS = 100;

    public PrioritizeJobStore(JobStatusWebSocketPublisher jobStatusWebSocketPublisher) {
        this.jobStatusWebSocketPublisher = jobStatusWebSocketPublisher;
    }

    public String createJob() {
        String jobId = "prioritize-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        jobs.put(jobId, PrioritizeJobStatus.pending(jobId));
        evictOldJobs();
        return jobId;
    }

    public void completeJob(String jobId, com.berdachuk.medexpertmatch.llm.service.MedicalAgentService.AgentResponse result) {
        PrioritizeJobStatus status = PrioritizeJobStatus.completed(jobId, result);
        jobs.put(jobId, status);
        jobStatusWebSocketPublisher.publish(jobId, status);
    }

    public void failJob(String jobId, String errorMessage) {
        PrioritizeJobStatus status = PrioritizeJobStatus.failed(jobId, errorMessage);
        jobs.put(jobId, status);
        jobStatusWebSocketPublisher.publish(jobId, status);
    }

    public PrioritizeJobStatus getStatus(String jobId) {
        return jobs.get(jobId);
    }

    public int purgeExpired(int completedFailedTtlMinutes, int pendingTtlMinutes) {
        long now = System.currentTimeMillis();
        long completedFailedCutoff = now - (long) completedFailedTtlMinutes * 60 * 1000;
        long pendingCutoff = now - (long) pendingTtlMinutes * 60 * 1000;

        int purged = jobs.size();
        jobs.entrySet().removeIf(entry -> {
            long jobTimestamp = extractTimestamp(entry.getKey());
            if (jobTimestamp < 0) return false;
            String status = entry.getValue().status();
            if (PrioritizeJobStatus.COMPLETED.equals(status) || PrioritizeJobStatus.FAILED.equals(status)) {
                return jobTimestamp < completedFailedCutoff;
            }
            if (PrioritizeJobStatus.PENDING.equals(status)) {
                return jobTimestamp < pendingCutoff;
            }
            return false;
        });
        return purged - jobs.size();
    }

    private long extractTimestamp(String jobId) {
        int firstDash = jobId.indexOf('-');
        int secondDash = jobId.indexOf('-', firstDash + 1);
        if (secondDash < 0) return -1;
        try {
            return Long.parseLong(jobId.substring(firstDash + 1, secondDash));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void evictOldJobs() {
        if (jobs.size() > MAX_JOBS) {
            jobs.keySet().stream()
                    .limit(jobs.size() - MAX_JOBS)
                    .forEach(jobs::remove);
        }
    }
}
