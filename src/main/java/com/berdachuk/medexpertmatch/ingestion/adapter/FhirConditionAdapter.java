package com.berdachuk.medexpertmatch.ingestion.adapter;

import org.hl7.fhir.r5.model.Condition;

import java.util.List;

/**
 * Adapter for extracting condition data and ICD-10 codes from FHIR Condition resources.
 */
public interface FhirConditionAdapter {

    /**
     * Extracts ICD-10 codes from a FHIR Condition resource.
     * Looks for codes with system "http://hl7.org/fhir/sid/icd-10".
     *
     * @param condition FHIR Condition resource
     * @return List of ICD-10 codes (e.g., ["I21.9", "E11.9"])
     */
    List<String> extractIcd10Codes(Condition condition);

    /**
     * Extracts SNOMED codes from a FHIR Condition resource.
     * Looks for codes with system "http://snomed.info/sct".
     *
     * @param condition FHIR Condition resource
     * @return List of SNOMED codes
     */
    List<String> extractSnomedCodes(Condition condition);

    /**
     * Extracts condition code text (diagnosis description).
     *
     * @param condition FHIR Condition resource
     * @return Condition code text, or null if not available
     */
    String extractCodeText(Condition condition);

    /**
     * Extracts condition severity.
     *
     * @param condition FHIR Condition resource
     * @return Severity text, or null if not available
     */
    String extractSeverity(Condition condition);

    /**
     * Extracts condition clinical status.
     *
     * @param condition FHIR Condition resource
     * @return Clinical status code, or null if not available
     */
    String extractClinicalStatus(Condition condition);
}
