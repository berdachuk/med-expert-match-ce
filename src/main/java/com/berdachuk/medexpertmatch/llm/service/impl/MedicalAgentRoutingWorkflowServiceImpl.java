package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.llm.agent.OrchestrationContextHolder;
import com.berdachuk.medexpertmatch.llm.harness.RoutingWorkflowEngine;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentRoutingWorkflowService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Facility routing workflow delegated to harness state machine (M30).
 */
@Slf4j
@Service
public class MedicalAgentRoutingWorkflowServiceImpl implements MedicalAgentRoutingWorkflowService {

    private final RoutingWorkflowEngine routingWorkflowEngine;
    private final LogStreamService logStreamService;

    public MedicalAgentRoutingWorkflowServiceImpl(
            RoutingWorkflowEngine routingWorkflowEngine,
            LogStreamService logStreamService) {
        this.routingWorkflowEngine = routingWorkflowEngine;
        this.logStreamService = logStreamService;
    }

    @Override
    public MedicalAgentService.AgentResponse routeCase(String caseId, Map<String, Object> request) {
        log.info("routeCase() called - caseId: {}", caseId);
        String sessionId = (String) request.getOrDefault("sessionId", "default");
        logStreamService.setCurrentSessionId(sessionId);
        OrchestrationContextHolder.setSessionId(sessionId);

        try {
            MedicalAgentService.AgentResponse response = routingWorkflowEngine.execute(caseId, request);
            logStreamService.logCompletion(sessionId, "Case routing", "Harness routing workflow finished");
            return response;
        } catch (Exception e) {
            log.error("Error in routeCase", e);
            logStreamService.logError(sessionId, "Case routing failed", e.getMessage());
            throw e;
        } finally {
            OrchestrationContextHolder.clear();
            logStreamService.clearCurrentSessionId();
        }
    }
}
