package com.berdachuk.medexpertmatch.llm.service;

import com.berdachuk.medexpertmatch.core.service.JobStatusWebSocketPublisher;
import com.berdachuk.medexpertmatch.llm.domain.MatchJobStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class JobStoreCleanupSchedulerTest {

    private MatchJobStore matchJobStore;
    private AnalyzeJobStore analyzeJobStore;
    private PrioritizeJobStore prioritizeJobStore;
    private RouteJobStore routeJobStore;
    private JobStoreCleanupScheduler scheduler;
    private JobStatusWebSocketPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = mock(JobStatusWebSocketPublisher.class);
        matchJobStore = new MatchJobStore(publisher);
        analyzeJobStore = new AnalyzeJobStore(publisher);
        prioritizeJobStore = new PrioritizeJobStore(publisher);
        routeJobStore = new RouteJobStore(publisher);
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
