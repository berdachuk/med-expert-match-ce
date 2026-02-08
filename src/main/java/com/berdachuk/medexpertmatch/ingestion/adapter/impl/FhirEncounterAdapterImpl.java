package com.berdachuk.medexpertmatch.ingestion.adapter.impl;

import com.berdachuk.medexpertmatch.ingestion.adapter.FhirEncounterAdapter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.Encounter;
import org.springframework.stereotype.Component;

/**
 * Implementation of FhirEncounterAdapter.
 * Extracts encounter data from FHIR Encounter resources.
 */
@Slf4j
@Component
public class FhirEncounterAdapterImpl implements FhirEncounterAdapter {

    @Override
    public String extractType(Encounter encounter) {
        if (encounter == null || !encounter.hasType() || encounter.getType().isEmpty()) {
            return null;
        }

        CodeableConcept type = encounter.getType().get(0); // Get first type
        if (type.hasText()) {
            return type.getText();
        }

        if (type.hasCoding() && !type.getCoding().isEmpty()) {
            Coding firstCoding = type.getCoding().get(0);
            if (firstCoding.hasDisplay()) {
                return firstCoding.getDisplay();
            }
            if (firstCoding.hasCode()) {
                return firstCoding.getCode();
            }
        }

        return null;
    }

    @Override
    public String extractStatus(Encounter encounter) {
        if (encounter == null || !encounter.hasStatus()) {
            return null;
        }

        return encounter.getStatus().toCode();
    }

    @Override
    public String extractClass(Encounter encounter) {
        if (encounter == null || !encounter.hasClass_() || encounter.getClass_().isEmpty()) {
            return null;
        }

        // getClass_() returns List<CodeableConcept> in FHIR R5
        CodeableConcept classConcept = encounter.getClass_().get(0);
        if (classConcept.hasCoding() && !classConcept.getCoding().isEmpty()) {
            Coding firstCoding = classConcept.getCoding().get(0);
            if (firstCoding.hasCode()) {
                return firstCoding.getCode();
            }
        }

        return null;
    }

    @Override
    public String extractServiceProvider(Encounter encounter) {
        if (encounter == null || !encounter.hasServiceProvider()) {
            return null;
        }

        String reference = encounter.getServiceProvider().getReference();
        if (reference == null || reference.isEmpty()) {
            return null;
        }

        // Extract ID from reference (e.g., "Organization/123" -> "123")
        if (reference.contains("/")) {
            return reference.substring(reference.lastIndexOf("/") + 1);
        }

        return reference;
    }
}
