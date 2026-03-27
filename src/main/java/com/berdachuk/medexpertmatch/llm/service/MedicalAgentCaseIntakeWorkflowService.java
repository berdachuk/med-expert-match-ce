package com.berdachuk.medexpertmatch.llm.service;

import java.util.Map;

/**
 * Workflow service for creating cases from raw text and matching doctors.
 */
public interface MedicalAgentCaseIntakeWorkflowService {

    /**
     * Creates a case from raw text and runs doctor matching.
     *
     * @param caseText The raw case text
     * @param request Request parameters
     * @return Agent response with created case and matching results
     */
    MedicalAgentService.AgentResponse matchFromText(String caseText, Map<String, Object> request);
}
