package com.berdachuk.medexpertmatch.llm.service;

import java.util.Map;

/**
 * Service for medical agent orchestration.
 * Orchestrates agent skills, tool invocations, and response generation.
 */
public interface MedicalAgentService {

    /**
     * Matches doctors to a medical case using case-analyzer and doctor-matcher skills.
     *
     * @param caseId  The medical case ID
     * @param request Optional request parameters
     * @return Agent response with matched doctors
     */
    AgentResponse matchDoctors(String caseId, Map<String, Object> request);

    /**
     * Prioritizes consultation queue using case-analyzer skill.
     *
     * @param request Request parameters (case IDs, filters, etc.)
     * @return Agent response with prioritized cases
     */
    AgentResponse prioritizeConsults(Map<String, Object> request);

    /**
     * Performs network analytics using network-analyzer skill.
     *
     * @param request Request parameters (condition codes, metrics, etc.)
     * @return Agent response with network analytics
     */
    AgentResponse networkAnalytics(Map<String, Object> request);

    /**
     * Analyzes a medical case using case-analyzer, evidence-retriever, and recommendation-engine skills.
     *
     * @param caseId  The medical case ID
     * @param request Optional request parameters
     * @return Agent response with case analysis and recommendations
     */
    AgentResponse analyzeCase(String caseId, Map<String, Object> request);

    /**
     * Generates expert recommendations using doctor-matcher skill.
     *
     * @param matchId The match ID
     * @param request Optional request parameters
     * @return Agent response with expert recommendations
     */
    AgentResponse generateRecommendations(String matchId, Map<String, Object> request);

    /**
     * Routes a case to facilities using case-analyzer and routing-planner skills.
     *
     * @param caseId  The medical case ID
     * @param request Optional request parameters
     * @return Agent response with facility routing recommendations
     */
    AgentResponse routeCase(String caseId, Map<String, Object> request);

    /**
     * Matches doctors from raw text (creates case, generates embeddings, matches in one call).
     *
     * @param caseText Case text (chief complaint) - required
     * @param request  Optional parameters (symptoms, additionalNotes, patientAge, caseType)
     * @return Agent response with matched doctors
     */
    AgentResponse matchFromText(String caseText, Map<String, Object> request);

    /**
     * Agent response containing the generated response and metadata.
     */
    record AgentResponse(
            String response,
            Map<String, Object> metadata
    ) {
    }
}
