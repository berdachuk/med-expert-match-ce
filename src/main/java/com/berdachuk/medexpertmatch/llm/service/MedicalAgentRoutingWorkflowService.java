package com.berdachuk.medexpertmatch.llm.service;

import java.util.Map;

/**
 * Workflow service for facility routing orchestration.
 */
public interface MedicalAgentRoutingWorkflowService {

    /**
     * Routes a case to facilities using retrieval tools and MedGemma summarization.
     *
     * @param caseId The medical case ID
     * @param request Request parameters
     * @return Agent response with routing recommendations
     */
    MedicalAgentService.AgentResponse routeCase(String caseId, Map<String, Object> request);
}
