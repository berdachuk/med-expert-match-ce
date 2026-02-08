package com.berdachuk.medexpertmatch.graph.service;

import java.util.List;

/**
 * Service interface for specialized graph queries used in Semantic Graph Retrieval.
 * This service encapsulates specific Cypher queries to improve code organization and maintainability.
 */
public interface GraphQueryService {

    /**
     * Calculates direct relationship score (doctor treated/consulted on this exact case).
     *
     * @param doctorId  Doctor ID
     * @param caseId    Medical case ID
     * @param sessionId Session ID for logging
     * @return Direct relationship score (0.0 to 1.0)
     */
    double calculateDirectRelationshipScore(String doctorId, String caseId, String sessionId);

    /**
     * Calculates condition expertise score (doctor treats conditions present in this case).
     *
     * @param doctorId   Doctor ID
     * @param icd10Codes List of ICD-10 codes from the medical case
     * @param sessionId  Session ID for logging
     * @return Condition expertise score (0.0 to 1.0)
     */
    double calculateConditionExpertiseScore(String doctorId, List<String> icd10Codes, String sessionId);

    /**
     * Calculates specialization match score (doctor specializes in case's required specialty).
     *
     * @param doctorId      Doctor ID
     * @param specialtyName Required specialty name
     * @param sessionId     Session ID for logging
     * @return Specialization match score (0.0 to 1.0)
     */
    double calculateSpecializationMatchScore(String doctorId, String specialtyName, String sessionId);

    /**
     * Calculates similar cases score (doctor treated cases with same ICD-10 codes).
     *
     * @param doctorId   Doctor ID
     * @param icd10Codes List of ICD-10 codes from the medical case
     * @param sessionId  Session ID for logging
     * @return Similar cases score (0.0 to 1.0)
     */
    double calculateSimilarCasesScore(String doctorId, List<String> icd10Codes, String sessionId);
}
