/**
 * Case Analysis module - Medical case analysis using the configured LLM.
 * <p>
 * This module provides:
 * - Domain entities (CaseAnalysisResult)
 * - Case analysis service using LLM models
 * - Entity extraction and urgency classification
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"core", "medicalcase"})
package com.berdachuk.medexpertmatch.caseanalysis;
