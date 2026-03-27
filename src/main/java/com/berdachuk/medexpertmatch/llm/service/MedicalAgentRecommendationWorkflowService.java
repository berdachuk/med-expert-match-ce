package com.berdachuk.medexpertmatch.llm.service;

import java.util.Map;

/**
 * Workflow service for recommendation generation orchestration.
 */
public interface MedicalAgentRecommendationWorkflowService {

    /**
     * Generates recommendations for a match or case context.
     *
     * @param matchId The match identifier
     * @param request Request parameters
     * @return Agent response with recommendations
     */
    MedicalAgentService.AgentResponse generateRecommendations(String matchId, Map<String, Object> request);
}
