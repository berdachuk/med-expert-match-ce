package com.berdachuk.medexpertmatch.llm.service;

/**
 * Shared LLM support for medical agent workflows.
 */
public interface MedicalAgentLlmSupportService {

    /**
     * Analyzes a medical case using the configured LLM.
     *
     * @param caseId The medical case ID
     * @return Case analysis result as text or JSON
     */
    String analyzeCaseWithMedGemma(String caseId);

    /**
     * Interprets tool results using the configured LLM.
     *
     * @param toolResults The tool execution results
     * @param caseAnalysis The case analysis context
     * @param patientAgeFromCase Authoritative patient age from the case
     * @return Final interpreted response
     */
    String interpretResultsWithMedGemma(String toolResults, String caseAnalysis, Integer patientAgeFromCase);

    /**
     * Summarizes routing results using the configured LLM.
     *
     * @param rawToolResults Raw routing tool results
     * @param caseAnalysis Case analysis context
     * @return Human-readable routing summary
     */
    String summarizeRoutingResults(String rawToolResults, String caseAnalysis);

    /**
     * Summarizes network analytics results using the configured LLM.
     *
     * @param rawResults Raw analytics results
     * @return Human-readable analytics summary
     */
    String summarizeNetworkAnalyticsResults(String rawResults);
}
