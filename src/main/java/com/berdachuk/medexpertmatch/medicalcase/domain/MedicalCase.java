package com.berdachuk.medexpertmatch.medicalcase.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Medical case domain entity.
 * Represents a medical case/patient case in the system.
 * <p>
 * Medical case IDs are internal MongoDB-compatible IDs (CHAR(24)).
 */
public record MedicalCase(
        String id,                    // CHAR(24) - internal MongoDB-compatible ID
        Integer patientAge,            // Anonymized patient age
        String chiefComplaint,
        String symptoms,
        String currentDiagnosis,
        List<String> icd10Codes,      // ICD-10 codes
        List<String> snomedCodes,    // SNOMED codes
        UrgencyLevel urgencyLevel,
        String requiredSpecialty,
        CaseType caseType,
        String additionalNotes,
        String abstractText,             // Comprehensive medical case abstract
        BigDecimal locationLatitude,
        BigDecimal locationLongitude
) {
    public MedicalCase(
            String id,
            Integer patientAge,
            String chiefComplaint,
            String symptoms,
            String currentDiagnosis,
            List<String> icd10Codes,
            List<String> snomedCodes,
            UrgencyLevel urgencyLevel,
            String requiredSpecialty,
            CaseType caseType,
            String additionalNotes,
            String abstractText) {
        this(
                id,
                patientAge,
                chiefComplaint,
                symptoms,
                currentDiagnosis,
                icd10Codes,
                snomedCodes,
                urgencyLevel,
                requiredSpecialty,
                caseType,
                additionalNotes,
                abstractText,
                null,
                null
        );
    }
}
