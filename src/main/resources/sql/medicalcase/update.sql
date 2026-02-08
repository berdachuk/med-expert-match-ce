UPDATE medexpertmatch.medical_cases
SET patient_age = :patientAge,
    chief_complaint = :chiefComplaint,
    symptoms = :symptoms,
    current_diagnosis = :currentDiagnosis,
    icd10_codes = :icd10Codes,
    snomed_codes = :snomedCodes,
    urgency_level = :urgencyLevel,
    required_specialty = :requiredSpecialty,
    case_type = :caseType,
    additional_notes = :additionalNotes,
    abstract = :abstract,
    updated_at = CURRENT_TIMESTAMP
WHERE id = :id
