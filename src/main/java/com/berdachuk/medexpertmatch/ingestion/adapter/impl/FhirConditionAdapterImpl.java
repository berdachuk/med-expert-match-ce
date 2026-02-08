package com.berdachuk.medexpertmatch.ingestion.adapter.impl;

import com.berdachuk.medexpertmatch.ingestion.adapter.FhirConditionAdapter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.Condition;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of FhirConditionAdapter.
 * Extracts condition data and ICD-10 codes from FHIR Condition resources.
 */
@Slf4j
@Component
public class FhirConditionAdapterImpl implements FhirConditionAdapter {

    private static final String ICD10_SYSTEM = "http://hl7.org/fhir/sid/icd-10";
    private static final String SNOMED_SYSTEM = "http://snomed.info/sct";

    @Override
    public List<String> extractIcd10Codes(Condition condition) {
        if (condition == null || !condition.hasCode()) {
            return List.of();
        }

        List<String> icd10Codes = new ArrayList<>();
        CodeableConcept code = condition.getCode();

        if (code.hasCoding()) {
            for (Coding coding : code.getCoding()) {
                if (ICD10_SYSTEM.equals(coding.getSystem())) {
                    String codeValue = coding.getCode();
                    if (codeValue != null && !codeValue.isEmpty()) {
                        icd10Codes.add(codeValue);
                    }
                }
            }
        }

        return icd10Codes;
    }

    @Override
    public List<String> extractSnomedCodes(Condition condition) {
        if (condition == null || !condition.hasCode()) {
            return List.of();
        }

        List<String> snomedCodes = new ArrayList<>();
        CodeableConcept code = condition.getCode();

        if (code.hasCoding()) {
            for (Coding coding : code.getCoding()) {
                if (SNOMED_SYSTEM.equals(coding.getSystem())) {
                    String codeValue = coding.getCode();
                    if (codeValue != null && !codeValue.isEmpty()) {
                        snomedCodes.add(codeValue);
                    }
                }
            }
        }

        return snomedCodes;
    }

    @Override
    public String extractCodeText(Condition condition) {
        if (condition == null || !condition.hasCode()) {
            return null;
        }

        CodeableConcept code = condition.getCode();
        if (code.hasText()) {
            return code.getText();
        }

        // Fallback to display text from first coding
        if (code.hasCoding() && !code.getCoding().isEmpty()) {
            Coding firstCoding = code.getCoding().get(0);
            if (firstCoding.hasDisplay()) {
                return firstCoding.getDisplay();
            }
        }

        return null;
    }

    @Override
    public String extractSeverity(Condition condition) {
        if (condition == null || !condition.hasSeverity()) {
            return null;
        }

        CodeableConcept severity = condition.getSeverity();
        if (severity.hasText()) {
            return severity.getText();
        }

        if (severity.hasCoding() && !severity.getCoding().isEmpty()) {
            Coding firstCoding = severity.getCoding().get(0);
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
    public String extractClinicalStatus(Condition condition) {
        if (condition == null || !condition.hasClinicalStatus()) {
            return null;
        }

        CodeableConcept status = condition.getClinicalStatus();
        if (status.hasCoding() && !status.getCoding().isEmpty()) {
            Coding firstCoding = status.getCoding().get(0);
            if (firstCoding.hasCode()) {
                return firstCoding.getCode();
            }
        }

        return null;
    }
}
