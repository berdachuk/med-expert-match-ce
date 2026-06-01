package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.llm.config.HarnessProperties;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class RecommendationWorkflowHandoffListener {

    private final MedicalAgentService medicalAgentService;
    private final HarnessProperties harnessProperties;

    public RecommendationWorkflowHandoffListener(
            MedicalAgentService medicalAgentService,
            HarnessProperties harnessProperties) {
        this.medicalAgentService = medicalAgentService;
        this.harnessProperties = harnessProperties;
    }

    @EventListener
    public void onDoctorMatchCompleted(DoctorMatchCompletedEvent event) {
        if (!harnessProperties.chainMatchToRecommend()) {
            return;
        }
        String handoffSessionId = event.sessionId() + "-recommend";
        String syntheticMatchId = "match-" + event.caseId();
        log.info("Chaining recommendations after doctor match caseId={} sessionId={}",
                event.caseId(), handoffSessionId);
        medicalAgentService.generateRecommendations(syntheticMatchId, Map.of("sessionId", handoffSessionId));
    }
}
