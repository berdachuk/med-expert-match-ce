package com.berdachuk.medexpertmatch.ingestion.adapter;

import org.hl7.fhir.r5.model.Encounter;

/**
 * Adapter for extracting encounter data from FHIR Encounter resources.
 */
public interface FhirEncounterAdapter {

    /**
     * Extracts encounter type.
     *
     * @param encounter FHIR Encounter resource
     * @return Encounter type code/text, or null if not available
     */
    String extractType(Encounter encounter);

    /**
     * Extracts encounter status.
     *
     * @param encounter FHIR Encounter resource
     * @return Encounter status code, or null if not available
     */
    String extractStatus(Encounter encounter);

    /**
     * Extracts encounter class.
     *
     * @param encounter FHIR Encounter resource
     * @return Encounter class code, or null if not available
     */
    String extractClass(Encounter encounter);

    /**
     * Extracts service provider reference.
     *
     * @param encounter FHIR Encounter resource
     * @return Service provider reference ID, or null if not available
     */
    String extractServiceProvider(Encounter encounter);
}
