SELECT id, patient_age, chief_complaint, symptoms, current_diagnosis, icd10_codes, snomed_codes, urgency_level, required_specialty, case_type, additional_notes, abstract
FROM medexpertmatch.medical_cases
WHERE :icd10Code = ANY(icd10_codes)
ORDER BY created_at DESC
LIMIT :maxResults
