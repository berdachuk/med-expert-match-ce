package com.berdachuk.medexpertmatch.ingestion.adapter;

import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import org.hl7.fhir.r5.model.Bundle;

/**
 * Adapter for converting FHIR Bundle resources to MedicalCase entities.
 * Parses Bundle entries (Patient, Condition, Observation, Encounter) and extracts medical case data.
 */
public interface FhirBundleAdapter {

    /**
     * Converts a FHIR Bundle to a MedicalCase entity.
     * Validates Bundle structure and resource references.
     * Extracts resources following FHIR R5 data types.
     *
     * @param bundle FHIR Bundle resource containing Patient, Condition, Observation, Encounter entries
     * @return MedicalCase entity, or null if Bundle cannot be converted
     * @throws IllegalArgumentException if Bundle structure is invalid or resources are missing
     */
    MedicalCase convertBundleToMedicalCase(Bundle bundle);

    /**
     * Validates that a FHIR Bundle has the required structure for conversion.
     *
     * @param bundle FHIR Bundle resource
     * @return true if Bundle is valid for conversion, false otherwise
     */
    boolean isValidBundle(Bundle bundle);
}
