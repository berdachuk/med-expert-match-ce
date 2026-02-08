package com.berdachuk.medexpertmatch.caseanalysis.service;

import com.berdachuk.medexpertmatch.caseanalysis.domain.CaseAnalysisResult;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;

import java.util.List;

/**
 * Service for analyzing medical cases using LLM.
 */
public interface CaseAnalysisService {

    /**
     * Performs comprehensive analysis of a medical case.
     *
     * @param medicalCase The medical case to analyze
     * @return Comprehensive case analysis result
     */
    CaseAnalysisResult analyzeCase(MedicalCase medicalCase);

    /**
     * Extracts ICD-10 codes from medical case text.
     *
     * @param medicalCase The medical case to extract codes from
     * @return List of ICD-10 codes
     */
    List<String> extractICD10Codes(MedicalCase medicalCase);

    /**
     * Classifies the urgency level of a medical case.
     *
     * @param medicalCase The medical case to classify
     * @return Urgency level
     */
    UrgencyLevel classifyUrgency(MedicalCase medicalCase);

    /**
     * Determines the required medical specialty(ies) for a case.
     *
     * @param medicalCase The medical case to analyze
     * @return List of required medical specialties (normalized names)
     */
    List<String> determineRequiredSpecialty(MedicalCase medicalCase);
}
