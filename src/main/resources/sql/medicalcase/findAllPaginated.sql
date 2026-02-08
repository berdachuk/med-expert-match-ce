SELECT id, patient_age, chief_complaint, symptoms, current_diagnosis,
       icd10_codes, snomed_codes, urgency_level, required_specialty,
       case_type, additional_notes, abstract, created_at
FROM medexpertmatch.medical_cases
ORDER BY created_at DESC
LIMIT :limit OFFSET :offset
