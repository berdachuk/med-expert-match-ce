package com.berdachuk.medexpertmatch.llm.service;

import java.util.Map;

/**
 * Workflow service for network analytics orchestration.
 */
public interface MedicalAgentNetworkAnalyticsWorkflowService {

    /**
     * Runs network analytics using graph and aggregation tools.
     *
     * @param request Request parameters
     * @return Agent response with analytics summary
     */
    MedicalAgentService.AgentResponse networkAnalytics(Map<String, Object> request);
}
