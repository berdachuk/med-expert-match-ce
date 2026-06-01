package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.llm.config.HarnessProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class DoctorMatchWorkflowHandoffListener {

    private final DoctorMatchWorkflowEngine doctorMatchWorkflowEngine;
    private final HarnessProperties harnessProperties;

    public DoctorMatchWorkflowHandoffListener(
            DoctorMatchWorkflowEngine doctorMatchWorkflowEngine,
            HarnessProperties harnessProperties) {
        this.doctorMatchWorkflowEngine = doctorMatchWorkflowEngine;
        this.harnessProperties = harnessProperties;
    }

    @EventListener
    public void onCaseAnalysisCompleted(CaseAnalysisCompletedEvent event) {
        if (!harnessProperties.chainAnalysisToMatch()) {
            return;
        }
        String handoffSessionId = event.sessionId() + "-match";
        log.info("Chaining doctor match after case analysis caseId={} sessionId={}",
                event.caseId(), handoffSessionId);
        doctorMatchWorkflowEngine.execute(event.caseId(), Map.of("sessionId", handoffSessionId));
    }
}
