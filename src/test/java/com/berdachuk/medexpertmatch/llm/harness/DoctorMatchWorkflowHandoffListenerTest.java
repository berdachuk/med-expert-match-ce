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

class DoctorMatchWorkflowHandoffListenerTest {

    @Test
    @DisplayName("chains doctor match when analysis handoff is enabled")
    void chainsWhenEnabled() {
        DoctorMatchWorkflowEngine engine = mock(DoctorMatchWorkflowEngine.class);
        HarnessProperties properties = new HarnessProperties(true, true, 2, true, 1, 0, false, true, false);
        DoctorMatchWorkflowHandoffListener listener =
                new DoctorMatchWorkflowHandoffListener(engine, properties);

        when(engine.execute(any(), any())).thenReturn(new MedicalAgentService.AgentResponse("ok", Map.of()));

        listener.onCaseAnalysisCompleted(new CaseAnalysisCompletedEvent(
                "6a1c68963a08e800010de68e", "sess-handoff", Instant.now()));

        verify(engine).execute(
                eq("6a1c68963a08e800010de68e"),
                argThat(req -> "sess-handoff-match".equals(req.get("sessionId"))));
    }

    @Test
    @DisplayName("skips handoff when disabled")
    void skipsWhenDisabled() {
        DoctorMatchWorkflowEngine engine = mock(DoctorMatchWorkflowEngine.class);
        HarnessProperties properties = HarnessProperties.defaults();
        DoctorMatchWorkflowHandoffListener listener =
                new DoctorMatchWorkflowHandoffListener(engine, properties);

        listener.onCaseAnalysisCompleted(new CaseAnalysisCompletedEvent(
                "6a1c68963a08e800010de68e", "sess-handoff", Instant.now()));

        verify(engine, never()).execute(any(), any());
    }
}
