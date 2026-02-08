package com.berdachuk.medexpertmatch.ingestion.adapter;

import org.hl7.fhir.r5.model.Patient;

/**
 * Adapter for extracting anonymized patient data from FHIR Patient resources.
 * Ensures no Protected Health Information (PHI) is extracted.
 */
public interface FhirPatientAdapter {

    /**
     * Extracts anonymized patient age from a FHIR Patient resource.
     * Only extracts age, never extracts identifiers, names, addresses, or other PHI.
     *
     * @param patient FHIR Patient resource
     * @return Patient age in years, or null if age cannot be determined
     */
    Integer extractAge(Patient patient);

    /**
     * Validates that the Patient resource does not contain PHI that should not be extracted.
     *
     * @param patient FHIR Patient resource
     * @return true if patient data is safe to extract (anonymized), false otherwise
     */
    boolean isAnonymized(Patient patient);
}
