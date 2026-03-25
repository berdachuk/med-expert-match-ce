/**
 * Case Analysis module - Medical case analysis using MedGemma.
 * <p>
 * This module provides:
 * - Domain entities (CaseAnalysisResult)
 * - Case analysis service using MedGemma models
 * - Entity extraction and urgency classification
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"core", "medicalcase"})
package com.berdachuk.medexpertmatch.caseanalysis;
