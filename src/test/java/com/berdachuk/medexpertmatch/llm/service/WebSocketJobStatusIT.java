package com.berdachuk.medexpertmatch.llm.service;

import com.berdachuk.medexpertmatch.core.service.JobStatusWebSocketPublisher;
import com.berdachuk.medexpertmatch.llm.domain.MatchJobStatus;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService.AgentResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * REQ-018: integration coverage for registered requirement.
 */
@ExtendWith(MockitoExtension.class)
class WebSocketJobStatusIT {

    @Mock
    private JobStatusWebSocketPublisher jobStatusWebSocketPublisher;

    @Test
    void completeJobPublishesWebSocketUpdate() {
        MatchJobStore store = new MatchJobStore(jobStatusWebSocketPublisher);
        String jobId = store.createJob();
        AgentResponse response = new AgentResponse("done", java.util.Map.of());

        store.completeJob(jobId, response);

        ArgumentCaptor<MatchJobStatus> captor = ArgumentCaptor.forClass(MatchJobStatus.class);
        verify(jobStatusWebSocketPublisher).publish(eq(jobId), captor.capture());
        assertEquals(MatchJobStatus.COMPLETED, captor.getValue().status());
    }

    @Test
    void failJobPublishesWebSocketUpdate() {
        MatchJobStore store = new MatchJobStore(jobStatusWebSocketPublisher);
        String jobId = store.createJob();

        store.failJob(jobId, "LLM timeout");

        ArgumentCaptor<MatchJobStatus> captor = ArgumentCaptor.forClass(MatchJobStatus.class);
        verify(jobStatusWebSocketPublisher).publish(eq(jobId), captor.capture());
        assertEquals(MatchJobStatus.FAILED, captor.getValue().status());
        assertEquals("LLM timeout", captor.getValue().errorMessage());
    }
}
