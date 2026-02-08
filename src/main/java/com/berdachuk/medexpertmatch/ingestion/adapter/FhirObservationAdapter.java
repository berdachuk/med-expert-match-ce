package com.berdachuk.medexpertmatch.ingestion.adapter;

import org.hl7.fhir.r5.model.Observation;

import java.util.List;

/**
 * Adapter for extracting observation data from FHIR Observation resources.
 */
public interface FhirObservationAdapter {

    /**
     * Extracts observation code text.
     *
     * @param observation FHIR Observation resource
     * @return Observation code text, or null if not available
     */
    String extractCodeText(Observation observation);

    /**
     * Extracts observation value as text.
     *
     * @param observation FHIR Observation resource
     * @return Observation value text, or null if not available
     */
    String extractValueText(Observation observation);

    /**
     * Extracts observation codes (for symptoms, findings, etc.).
     *
     * @param observation FHIR Observation resource
     * @return List of observation code texts
     */
    List<String> extractCodes(Observation observation);
}
