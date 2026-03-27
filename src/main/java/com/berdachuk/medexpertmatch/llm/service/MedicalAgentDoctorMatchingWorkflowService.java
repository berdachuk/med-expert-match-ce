package com.berdachuk.medexpertmatch.llm.service;

import java.util.Map;

/**
 * Workflow service for doctor matching orchestration.
 */
public interface MedicalAgentDoctorMatchingWorkflowService {

    /**
     * Matches doctors to a case using MedGemma analysis and retrieval tools.
     *
     * @param caseId The medical case ID
     * @param request Request parameters
     * @return Agent response with matched doctors
     */
    MedicalAgentService.AgentResponse matchDoctors(String caseId, Map<String, Object> request);
}
