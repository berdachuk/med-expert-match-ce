package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.llm.config.HarnessProperties;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecommendationWorkflowHandoffListenerTest {

    @Test
    @DisplayName("chains recommendations when match handoff is enabled")
    void chainsWhenEnabled() {
        MedicalAgentService agentService = mock(MedicalAgentService.class);
        HarnessProperties properties = new HarnessProperties(true, true, 2, true, 1, 0, false, false, true, false, true);
        RecommendationWorkflowHandoffListener listener =
                new RecommendationWorkflowHandoffListener(agentService, properties, mock(org.springframework.context.ApplicationEventPublisher.class));
        when(agentService.generateRecommendations(any(), any()))
                .thenReturn(new MedicalAgentService.AgentResponse("ok", Map.of()));

        listener.onDoctorMatchCompleted(new DoctorMatchCompletedEvent(
                "6a1c68963a08e800010de68e", "sess-match", Instant.now()));

        verify(agentService).generateRecommendations(
                eq("match-6a1c68963a08e800010de68e"),
                argThat(req -> "sess-match-recommend".equals(req.get("sessionId"))));
    }

    @Test
    @DisplayName("skips recommendation handoff when disabled")
    void skipsWhenDisabled() {
        MedicalAgentService agentService = mock(MedicalAgentService.class);
        RecommendationWorkflowHandoffListener listener =
                new RecommendationWorkflowHandoffListener(agentService, HarnessProperties.defaults(),
                        mock(org.springframework.context.ApplicationEventPublisher.class));

        listener.onDoctorMatchCompleted(new DoctorMatchCompletedEvent(
                "6a1c68963a08e800010de68e", "sess-match", Instant.now()));

        verify(agentService, never()).generateRecommendations(any(), any());
    }
}
