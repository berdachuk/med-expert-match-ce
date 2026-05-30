package com.berdachuk.medexpertmatch.llm.service;

import com.berdachuk.medexpertmatch.llm.domain.MatchJobStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class JobStoreCleanupSchedulerTest {

    private MatchJobStore matchJobStore;
    private AnalyzeJobStore analyzeJobStore;
    private PrioritizeJobStore prioritizeJobStore;
    private RouteJobStore routeJobStore;
    private JobStoreCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        matchJobStore = new MatchJobStore();
        analyzeJobStore = new AnalyzeJobStore();
        prioritizeJobStore = new PrioritizeJobStore();
        routeJobStore = new RouteJobStore();
        scheduler = new JobStoreCleanupScheduler(matchJobStore, analyzeJobStore, prioritizeJobStore, routeJobStore);
    }

    @Test
    void purgeStaleJobsRemovesExpiredCompletedJobs() throws InterruptedException {
        String jobId = matchJobStore.createJob();
        matchJobStore.completeJob(jobId, new com.berdachuk.medexpertmatch.llm.service.MedicalAgentService.AgentResponse("ok", null));

        Thread.sleep(5);
        matchJobStore.purgeExpired(0, 60);

        assertNull(matchJobStore.getStatus(jobId));
    }

    @Test
    void purgeStaleJobsRunsWithoutError() {
        String jobId = matchJobStore.createJob();
        scheduler.purgeStaleJobs();
        assertNotNull(matchJobStore.getStatus(jobId));
    }
}
