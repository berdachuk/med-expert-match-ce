package com.berdachuk.medexpertmatch.llm.service;

import java.util.Map;

/**
 * Workflow service for consultation queue prioritization.
 */
public interface MedicalAgentQueuePrioritizationWorkflowService {

    /**
     * Prioritizes consultations based on case urgency and context.
     *
     * @param request Request parameters
     * @return Agent response with prioritized cases
     */
    MedicalAgentService.AgentResponse prioritizeConsults(Map<String, Object> request);
}
