package com.berdachuk.medexpertmatch.ingestion.service;

import org.hl7.fhir.r5.model.Bundle;

import java.util.List;

/**
 * Service for generating medical cases.
 */
public interface MedicalCaseGeneratorService {

    /**
     * Generates FHIR R5 compliant bundles containing Patient, Condition, Observation, and Encounter.
     *
     * @param count              Number of bundles to generate
     * @param icd10Codes         List of ICD-10 codes (loaded from CSV)
     * @param extendedIcd10Codes Extended list of ICD-10 codes
     * @param symptoms           List of symptoms (loaded from CSV)
     * @param severities         List of severities (loaded from CSV)
     * @param encounterClasses   List of encounter classes (loaded from CSV)
     * @param encounterTypes     List of encounter types (loaded from CSV)
     * @return List of FHIR bundles
     */
    List<Bundle> generateFhirBundles(int count,
                                     List<String> icd10Codes, List<String> extendedIcd10Codes,
                                     List<String> symptoms, List<String> severities,
                                     List<String> encounterClasses, List<String> encounterTypes);

    /**
     * Generates medical cases from FHIR Bundles.
     *
     * @param count              Number of cases to generate
     * @param progress           Optional progress tracker
     * @param icd10Codes         List of ICD-10 codes (loaded from CSV)
     * @param extendedIcd10Codes Extended list of ICD-10 codes
     * @param symptoms           List of symptoms (loaded from CSV)
     * @param severities         List of severities (loaded from CSV)
     * @param encounterClasses   List of encounter classes (loaded from CSV)
     * @param encounterTypes     List of encounter types (loaded from CSV)
     */
    void generateMedicalCases(int count, SyntheticDataGenerationProgress progress,
                              List<String> icd10Codes, List<String> extendedIcd10Codes,
                              List<String> symptoms, List<String> severities,
                              List<String> encounterClasses, List<String> encounterTypes);
}
