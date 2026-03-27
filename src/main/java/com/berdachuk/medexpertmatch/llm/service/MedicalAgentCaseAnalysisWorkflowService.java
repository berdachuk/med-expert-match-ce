package com.berdachuk.medexpertmatch.llm.service;

import java.util.Map;

/**
 * Workflow service for case analysis orchestration.
 */
public interface MedicalAgentCaseAnalysisWorkflowService {

    /**
     * Analyzes a case and augments it with evidence retrieval.
     *
     * @param caseId The medical case ID
     * @param request Request parameters
     * @return Agent response with analysis and evidence
     */
    MedicalAgentService.AgentResponse analyzeCase(String caseId, Map<String, Object> request);
}
