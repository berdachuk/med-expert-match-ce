package com.berdachuk.medexpertmatch.ingestion.adapter.impl;

import com.berdachuk.medexpertmatch.ingestion.adapter.FhirObservationAdapter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.Observation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of FhirObservationAdapter.
 * Extracts observation data from FHIR Observation resources.
 */
@Slf4j
@Component
public class FhirObservationAdapterImpl implements FhirObservationAdapter {

    @Override
    public String extractCodeText(Observation observation) {
        if (observation == null || !observation.hasCode()) {
            return null;
        }

        CodeableConcept code = observation.getCode();
        if (code.hasText()) {
            return code.getText();
        }

        if (code.hasCoding() && !code.getCoding().isEmpty()) {
            Coding firstCoding = code.getCoding().get(0);
            if (firstCoding.hasDisplay()) {
                return firstCoding.getDisplay();
            }
        }

        return null;
    }

    @Override
    public String extractValueText(Observation observation) {
        if (observation == null || !observation.hasValue()) {
            return null;
        }

        // Extract value as string representation
        return observation.getValue().primitiveValue();
    }

    @Override
    public List<String> extractCodes(Observation observation) {
        if (observation == null || !observation.hasCode()) {
            return List.of();
        }

        List<String> codes = new ArrayList<>();
        CodeableConcept code = observation.getCode();

        if (code.hasCoding()) {
            for (Coding coding : code.getCoding()) {
                if (coding.hasDisplay()) {
                    codes.add(coding.getDisplay());
                } else if (coding.hasCode()) {
                    codes.add(coding.getCode());
                }
            }
        }

        if (code.hasText() && !codes.contains(code.getText())) {
            codes.add(code.getText());
        }

        return codes;
    }
}
